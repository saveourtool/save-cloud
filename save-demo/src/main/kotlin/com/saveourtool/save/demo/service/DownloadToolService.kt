package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.config.CustomCoroutineDispatchers
import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.demo.storage.toToolKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.utils.*
import com.saveourtool.save.utils.github.GitHubHelper.download
import com.saveourtool.save.utils.github.GitHubHelper.queryMetadata
import com.saveourtool.save.utils.github.ReleaseAsset

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

import java.nio.ByteBuffer
import javax.annotation.PostConstruct

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
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
    private val coroutineDispatchers: CustomCoroutineDispatchers,
) {
    private val scope: CoroutineScope = CoroutineScope(coroutineDispatchers.io)
    private val httpClient = httpClient()


    private suspend fun downloadFileByFileId(fileId: Long): Flow<ByteBuffer> = httpClient.post {
        url("${configProperties.backendUrl}/files/download?fileId=$fileId")
        accept(ContentType.Application.OctetStream)
    }
        .bodyAsChannel()
        .toByteBufferFlow()

    /**
     * @param repo
     * @param vcsTagName
     * @return [DisposableHandle]
     */
    fun downloadFromGithubAndUploadToStorage(repo: GithubRepo, vcsTagName: String) = scope.launch {
        val asset = getExecutable(repo, vcsTagName)
        download(repo.toDto(), vcsTagName, asset.name) { (content, size) ->
            val dependency =
                dependencyStorage.findDependency(repo.organizationName, repo.projectName, vcsTagName, asset.name)
                    ?: run {
                        withContext(coroutineDispatchers.io) {
                            demoService.findBySaveourtoolProjectOrNotFound(repo.organizationName, repo.projectName) {
                                "Not found demo for ${repo.toPrettyString()}"
                            }
                                .let {
                                    Dependency(it, vcsTagName, asset.name, -1L)
                                }
                        }
                    }
            dependencyStorage.overwrite(dependency, size, content.toByteBufferFlow())
        }
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

    @PostConstruct
    private fun loadToStorage() {
        scope.launch(coroutineDispatchers.io) {
            val tools = toolService.getSupportedTools()
                .map { it.toToolKey() }
                .plus(DiktatDemoTool.DIKTAT.toToolKey("diktat-1.2.3.jar"))
                .plus(DiktatDemoTool.KTLINT.toToolKey("ktlint"))
                .filterNot {
                    dependencyStorage.doesExist(it.organizationName, it.projectName, it.version, it.fileName)
                }
            if (tools.isEmpty()) {
                logger.debug("All required tools are already present in storage.")
            } else {
                val toolsToBeDownloaded = tools.joinToString(", ") { it.toPrettyString() }
                logger.info("Tools to be downloaded: [$toolsToBeDownloaded]")
            }
            tools.forEach { tool ->
                downloadFromGithubAndUploadToStorage(
                    GithubRepo(tool.organizationName, tool.projectName),
                    tool.version,
                )
            }
        }
    }

    private suspend fun getExecutable(repo: GithubRepo, vcsTagName: String) = queryMetadata(repo.toDto(), vcsTagName).assets
        .filterNot(ReleaseAsset::isDigest)
        .first()

    /**
     * Perform GitHub tool download if [githubProjectCoordinates] is not null
     *
     * @param githubProjectCoordinates
     * @param vcsTagName
     * @return [Tool] that has been downloaded
     */
    suspend fun initializeGithubDownload(githubProjectCoordinates: ProjectCoordinates?, vcsTagName: String): Tool? =
        githubProjectCoordinates?.toGithubRepo()?.let {githubRepo ->
            val tool = withContext(coroutineDispatchers.io) {
                val repo = githubRepoService.saveIfNotPresent(githubRepo)
                val snapshot = Snapshot(vcsTagName, getExecutableName(repo, vcsTagName))
                    .let { snapshotService.saveIfNotPresent(it) }
                toolService.saveIfNotPresent(repo, snapshot)
            }
            if (tool.id.isNotNull()) {
                downloadFromGithubAndUploadToStorage(tool.githubRepo, tool.snapshot.version)
            }
            tool
        }

    /**
     * Get name of GitHub downloaded asset
     *
     * @param repo GitHub repo coordinates
     * @param vcsTagName string that represents the version control system tag
     * @return executable name
     */
    fun getExecutableName(repo: GithubRepo, vcsTagName: String) = runBlocking(coroutineDispatchers.io) {
        getExecutable(repo, vcsTagName).name
    }

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
        dependencyStorage.overwrite(dependency, downloadFileByFileId(dependency.fileId))
        with(dependency) {
            logger.debug("Successfully downloaded $fileName for ${demo.organizationName}/${demo.projectName}.")
        }
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<DownloadToolService>()
        private fun httpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true }
                json(json)
            }
        }
    }
}
