package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.demo.storage.toToolKey
import com.saveourtool.save.demo.utils.toByteBufferFlux
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.utils.asyncEffectIf
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.getLogger

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.CancellationException
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
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
class DownloadToolService(
    private val dependencyStorage: DependencyStorage,
    private val toolService: ToolService,
    private val githubRepoService: GithubRepoService,
    private val snapshotService: SnapshotService,
    private val configProperties: ConfigProperties,
    private val demoService: DemoService,
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

    private suspend fun downloadAsset(asset: ReleaseAsset): Flux<ByteBuffer> = httpClient.get {
        url(asset.downloadUrl)
        accept(asset.contentType())
    }
        .bodyAsChannel()
        .toByteBufferFlux()

    private suspend fun downloadFileByFileId(fileId: Long): Flux<ByteBuffer> = httpClient.post {
        url("${configProperties.backendUrl}/files/download?fileId=$fileId")
        accept(ContentType.Application.OctetStream)
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
                val content = downloadAsset(asset)
                dependencyStorage.findDependency(repo.organizationName, repo.projectName, vcsTagName, asset.name)
                    .switchIfEmpty {
                        demoService.findBySaveourtoolProjectOrNotFound(repo.organizationName, repo.projectName) {
                            "Not found demo for ${repo.toPrettyString()}"
                        }
                            .map {
                                Dependency(it, vcsTagName, asset.name, -1L)
                            }
                    }
                    .flatMap { dependencyStorage.usingProjectReactor().overwrite(it, content) }
                    .subscribe()
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
    private fun loadToStorage() = toolService.getSupportedTools()
        .map { it.toToolKey() }
        .plus(DiktatDemoTool.DIKTAT.toToolKey("diktat-1.2.3.jar"))
        .plus(DiktatDemoTool.KTLINT.toToolKey("ktlint"))
        .toFlux()
        .filterWhen {
            dependencyStorage.doesExist(it.organizationName, it.projectName, it.version, it.version).map(Boolean::not)
        }
        .collectList()
        .doOnNext { tools ->
            if (tools.isEmpty()) {
                logger.debug("All required tools are already present in storage.")
            } else {
                val toolsToBeDownloaded = tools.joinToString(", ") { it.toPrettyString() }
                logger.info("Tools to be downloaded: [$toolsToBeDownloaded]")
            }
        }
        .flatMapIterable { it }
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
        .asyncEffectIf({ this.id != null }) {
            blockingToMono { downloadFromGithubAndUploadToStorage(it.githubRepo, it.snapshot.version) }
        }

    /**
     * Get name of GitHub downloaded asset
     *
     * @param repo GitHub repo coordinates
     * @param vcsTagName string that represents the version control system tag
     * @return executable name
     */
    fun getExecutableName(repo: GithubRepo, vcsTagName: String) = getExecutable(repo, vcsTagName).name

    /**
     * Upload several [dependencies] to storage (will be overwritten if already present)
     *
     * @param dependencies list of [Dependency] to store in [dependencyStorage]
     * @return number of dependencies that has been downloaded
     */
    fun downloadToStorage(dependencies: List<Dependency>) = dependencies.map { downloadToStorage(it) }
        .size
        .also { logger.info("Successfully downloaded $it files from file storage.") }

    /**
     * Upload [dependency] to storage (will be overwritten if already present)
     *
     * @param dependency that should be saved to [dependencyStorage]
     * @return [Job]
     */
    fun downloadToStorage(dependency: Dependency) = scope.launch {
        downloadFileByFileId(dependency.fileId)
            .let { byteBuffers ->
                with(dependency) {
                    dependencyStorage.usingProjectReactor().overwrite(dependency, byteBuffers)
                        .subscribe()
                        .also { logger.debug("Successfully downloaded $fileName for ${demo.organizationName}/${demo.projectName}.") }
                }
            }
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<DownloadToolService>()
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
