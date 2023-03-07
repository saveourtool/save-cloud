package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.utils.*
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
import org.springframework.stereotype.Service

import java.nio.ByteBuffer

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
    private val blockingBridge: BlockingBridge,
) {
    private val httpClient = httpClient()

    private suspend fun downloadFileByFileId(fileId: Long): Flow<ByteBuffer> = httpClient.post {
        url("${configProperties.backendUrl}/files/download?fileId=$fileId")
        accept(ContentType.Application.OctetStream)
    }
        .bodyAsChannel()
        .toByteBufferFlow()

    /**
     * Perform GitHub tool download if [githubProjectCoordinates] is not null
     *
     * @param githubProjectCoordinates
     * @param vcsTagName
     * @return [Tool] that has been downloaded
     */
    suspend fun initializeGithubDownload(githubProjectCoordinates: ProjectCoordinates?, vcsTagName: String): Tool? =
            githubProjectCoordinates?.toGithubRepo()?.let {githubRepo ->
                val repo = blockingBridge.blockingToSuspend {
                    githubRepoService.saveIfNotPresent(githubRepo)
                }
                val snapshot = Snapshot(vcsTagName, getExecutableName(repo, vcsTagName))
                val tool = blockingBridge.blockingToSuspend {
                    toolService.saveIfNotPresent(repo, snapshotService.saveIfNotPresent(snapshot))
                }
                if (tool.id.isNotNull()) {
                    dependencyStorage.uploadFromGitHub(tool.githubRepo, tool.snapshot.version)
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
    suspend fun getExecutableName(repo: GithubRepo, vcsTagName: String) = queryMetadata(repo, vcsTagName)
        .assets
        .filterNot(ReleaseAsset::isDigest)
        .first()
        .name

    /**
     * Upload [dependency] to storage (will be overwritten if already present)
     *
     * @param backendFile [FileDto] taken from backend
     * @param dependency that should be saved to [dependencyStorage]
     * @return updated [Dependency]
     */
    suspend fun downloadToStorage(backendFile: FileDto, dependency: Dependency): Unit = run {
        require(backendFile.requiredId() == dependency.fileId) {
            "Invalid link between backend file $backendFile and dependency $dependency"
        }
        require(backendFile.sizeBytes > 0) {
            "Invalid content length of backend file $backendFile"
        }
        dependencyStorage.overwrite(dependency, backendFile.sizeBytes, downloadFileByFileId(backendFile.requiredId()))
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
