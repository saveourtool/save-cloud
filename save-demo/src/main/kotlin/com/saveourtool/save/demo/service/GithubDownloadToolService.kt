package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.demo.utils.toByteBufferFlux
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.utils.asyncEffect
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.getLogger

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.CancellationException
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.nio.ByteBuffer
import javax.annotation.PostConstruct

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Service that downloads tools from GitHub when starting
 */
@Service
class GithubDownloadToolService(
    private val toolStorage: ToolStorage,
    private val toolService: ToolService,
    private val githubRepoService: GithubRepoService,
    private val snapshotService: SnapshotService,
) {
    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    @Suppress("InjectDispatcher")
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val httpClient = httpClient()

    private fun getGithubMetadataUrl(repo: GithubRepo, vcsTagName: String) = if (vcsTagName == LATEST_VERSION) {
        vcsTagName
    } else {
        "tags/$vcsTagName"
    }
        .let { release ->
            "$GITHUB_API_URL/${repo.organizationName}/${repo.projectName}/releases/$release"
        }

    private fun getMetadata(repo: GithubRepo, vcsTagName: String): ReleaseMetadata {
        val channel: Channel<ReleaseMetadata> = Channel()
        scope.launch {
            httpClient.get(getGithubMetadataUrl(repo, vcsTagName))
                .bodyAsText()
                .let {
                    jsonSerializer.decodeFromString<ReleaseMetadata>(it)
                }
                .let {
                    channel.send(it)
                    channel.close()
                }
        }
        return runBlocking { channel.receive() }
    }

    @Suppress("ReactiveStreamsUnusedPublisher")
    private suspend fun downloadAsset(asset: ReleaseAsset): Flux<ByteBuffer> = httpClient.get {
        url(asset.downloadUrl)
        accept(asset.contentType())
    }
        .bodyAsChannel()
        .toByteBufferFlux()

    /**
     * @param repo
     * @param vcsTagName
     * @return [DisposableHandle]
     */
    fun downloadFromGithubAndUploadToStorage(repo: GithubRepo, vcsTagName: String) = getExecutable(repo, vcsTagName)
        .let { asset ->
            scope.launch {
                toolStorage.overwrite(
                    ToolKey(repo.organizationName, repo.projectName, vcsTagName, asset.name),
                    downloadAsset(asset),
                ).subscribe()
            }
                .invokeOnCompletion { exception ->
                    exception?.let {
                        if (it is CancellationException) {
                            logger.debug("Download of ${repo.toPrettyString()} was cancelled.")
                        } else {
                            logger.error("Error while downloading ${repo.toPrettyString()}: ${it.message}")
                        }
                    } ?: logger.debug("${repo.toPrettyString()} was successfully downloaded.")
                }
        }

    @PostConstruct
    private fun loadToStorage() = toolStorage.list()
        .collectList()
        .zipWith(toolService.getSupportedTools().toMono())
        .flatMapIterable { (availableFiles, supportedTools) ->
            supportedTools.map { it.toToolKey() }
                .filter { it !in availableFiles }
                .also { tools ->
                    if (tools.isEmpty()) {
                        logger.debug("All required tools are already present in storage.")
                    } else {
                        val toolsToBeDownloaded = tools.joinToString(", ") { it.toPrettyString() }
                        logger.info("Tools to be downloaded: [$toolsToBeDownloaded]")
                    }
                }
        }
        .flatMap { key ->
            downloadFromGithubAndUploadToStorage(
                GithubRepo(key.organizationName, key.projectName),
                key.version,
            ).toMono()
        }
        .subscribe()

    private fun getExecutable(repo: GithubRepo, vcsTagName: String) = getMetadata(repo, vcsTagName).assets
        .filterNot(ReleaseAsset::isDigest)
        .first()

    /**
     * Perform GitHub tool download if [githubProjectCoordinates] is not null
     *
     * @param githubProjectCoordinates
     * @param vcsTagName
     * @return [Tool] that has been downloaded
     */
    fun initializeGithubDownload(githubProjectCoordinates: ProjectCoordinates?, vcsTagName: String): Mono<Tool> = blockingToMono {
        githubProjectCoordinates?.toGithubRepo()?.let { githubRepoService.saveIfNotPresent(it) }
    }
        .zipWhen { repo ->
            Snapshot(vcsTagName, getExecutableName(repo, vcsTagName))
                .let { blockingToMono { snapshotService.saveIfNotPresent(it) } }
        }
        .map { (repo, snapshot) ->
            toolService.saveIfNotPresent(repo, snapshot)
        }
        .asyncEffect {
            blockingToMono { downloadFromGithubAndUploadToStorage(it.githubRepo, it.snapshot.version) }
        }

    /**
     * @param repo
     * @param vcsTagName
     * @return executable name
     */
    fun getExecutableName(repo: GithubRepo, vcsTagName: String) = getExecutable(repo, vcsTagName).name

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<GithubDownloadToolService>()
        private const val GITHUB_API_URL = "https://api.github.com/repos"
        private const val LATEST_VERSION = "latest"
        private fun httpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true }
                json(json)
            }
        }
    }
}
