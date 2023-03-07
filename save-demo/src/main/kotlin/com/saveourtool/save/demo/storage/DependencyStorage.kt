package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.entity.GithubRepo
import com.saveourtool.save.demo.repository.DependencyRepository
import com.saveourtool.save.demo.service.DemoService
import com.saveourtool.save.demo.service.ToolService
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.DefaultStorageCoroutines
import com.saveourtool.save.storage.StorageCoroutines
import com.saveourtool.save.storage.SuspendingStorageWithDatabase
import com.saveourtool.save.utils.*
import com.saveourtool.save.utils.github.GitHubHelper.downloadAsset
import com.saveourtool.save.utils.github.GitHubHelper.queryMetadata
import com.saveourtool.save.utils.github.ReleaseAsset

import org.slf4j.Logger
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

import java.nio.ByteBuffer
import java.nio.file.Path

import kotlin.io.path.*
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.reactive.asFlow

/**
 * Storage to keep all the tools on the disk
 */
@Component
class DependencyStorage(
    s3Operations: S3Operations,
    repository: DependencyRepository,
    s3KeyManager: DependencyKeyManager,
    private val toolService: ToolService,
    private val demoService: DemoService,
) : SuspendingStorageWithDatabase<Dependency, Dependency, DependencyRepository, DependencyKeyManager>(
    s3Operations,
    s3KeyManager,
    repository,
) {
    override suspend fun doInit(underlying: DefaultStorageCoroutines<Dependency>) {
        super.doInit(underlying)
        val tools = toolService.getSupportedTools()
            .map { it.toToolKey() }
            .plus(DiktatDemoTool.DIKTAT.toToolKey("diktat-1.2.3.jar"))
            .plus(DiktatDemoTool.KTLINT.toToolKey("ktlint"))
            .filter { toolKey ->
                doesExist(
                    toolKey.organizationName,
                    toolKey.projectName,
                    toolKey.version,
                    toolKey.fileName,
                )
            }
        if (tools.isEmpty()) {
            log.debug("All required tools are already present in storage.")
        } else {
            val toolsToBeDownloaded = tools.joinToString(", ") { it.toPrettyString() }
            log.info("Tools to be downloaded: [$toolsToBeDownloaded]")
        }
        tools.forEach { tool ->
            underlying.downloadFromGithubAndUploadToStorage(
                GithubRepo(tool.organizationName, tool.projectName),
                tool.version,
            )
        }
    }

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @return list of files present in storage for required version
     */
    fun blockingList(
        demo: Demo,
        version: String,
    ): List<Dependency> = s3KeyManager.blockingFindAllKeys(
        demo.organizationName,
        demo.projectName,
        version,
    )

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @return list of files present in storage for required version
     */
    suspend fun list(
        demo: Demo,
        version: String,
    ): List<Dependency> = s3KeyManager.findAllKeys(
        demo.organizationName,
        demo.projectName,
        version,
    )

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @param fileName name of a file to be deleted
     * @return true if file is successfully deleted, false otherwise
     */
    suspend fun delete(
        demo: Demo,
        version: String,
        fileName: String,
    ): Boolean? {
        val dependency = findDependency(demo.organizationName, demo.projectName, version, fileName)
        val deleted = dependency?.let { delete(it) }
        if (deleted == true) {
            log.debug {
                "Deleted $fileName associated with version $version from $demo"
            }
        }
        return deleted
    }

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     * @return true if storage contains some dependency with provided values, otherwise -- false
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    suspend fun doesExist(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    ): Boolean = findDependency(organizationName, projectName, version, fileName).isNotNull()

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     * @return [Dependency] found by provided values
     */
    suspend fun findDependency(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    ): Dependency? = s3KeyManager.findKey(
        organizationName,
        projectName,
        version,
        fileName,
    )

    private suspend fun downloadToTempDir(
        tempDir: Path,
        organizationName: String,
        projectName: String,
        version: String
    ) = s3KeyManager.findAllKeys(organizationName, projectName, version)
        .map {
            download(it).collectToFile(tempDir / it.fileName)
        }
        .last()

    /**
     * Get all the files of requested [version] of [organizationName]/[projectName] demo as zip archive
     *
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of the demo
     * @param archiveName name that an archive should have
     * @return [Flux] with zip as content
     */
    @OptIn(ExperimentalPathApi::class)
    suspend fun archive(
        organizationName: String,
        projectName: String,
        version: String,
        archiveName: String = "archive.zip",
    ): Flow<ByteBuffer> = createTempDirectory().div("archive").createDirectory().let { tempDir ->
        createArchive(tempDir, organizationName, projectName, version, archiveName)
            .onCompletion {
                tempDir.deleteRecursively()
            }
    }

    private suspend fun createArchive(
        tmpDir: Path,
        organizationName: String,
        projectName: String,
        version: String,
        archiveName: String = "archive.zip",
    ): Flow<ByteBuffer> =
            downloadToTempDir(tmpDir, organizationName, projectName, version)
                .let {
                    tmpDir.parent.div(archiveName)
                        .also { dirToZip -> tmpDir.compressAsZipTo(dirToZip) }
                }.toByteBufferFlux().asFlow()

    /**
     * @param repo
     * @param vcsTagName
     * @return [DisposableHandle]
     */
    suspend fun uploadFromGitHub(repo: GithubRepo, vcsTagName: String) {
        this.downloadFromGithubAndUploadToStorage(repo, vcsTagName)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun StorageCoroutines<Dependency>.downloadFromGithubAndUploadToStorage(
        repo: GithubRepo,
        vcsTagName: String
    ) {
        val asset = queryMetadata(repo, vcsTagName)
            .assets
            .filterNot(ReleaseAsset::isDigest)
            .first()
        val dependency = s3KeyManager.findKey(
            repo.organizationName,
            repo.projectName,
            vcsTagName,
            asset.name,
        ) ?: run {
            val demo = demoService.findBySaveourtoolProjectOrNotFound(repo.organizationName, repo.projectName) {
                "Not found demo for ${repo.toPrettyString()}"
            }
            Dependency(demo, vcsTagName, asset.name, -1L)
        }

        try {
            downloadAsset(asset) { content ->
                overwrite(dependency, asset.size, content.toByteBufferFlow())
            }
            log.debug("${repo.toPrettyString()} was successfully downloaded.")
        } catch (ex: Exception) {
            log.error("Error while downloading ${repo.toPrettyString()}: ${ex.message}")
        }
    }
    companion object {
        private val log: Logger = getLogger<DependencyStorage>()
    }
}
