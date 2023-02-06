package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.repository.DependencyRepository
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.AbstractStorageWithDatabaseEntityKey
import com.saveourtool.save.utils.*
import org.slf4j.Logger
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.*

private const val TOOLS_PATH = "tools"

/**
 * Storage to keep all the tools on the disk
 */
@Component
class DependencyStorage(
    s3Operations: S3Operations,
    repository: DependencyRepository,
) : AbstractStorageWithDatabaseEntityKey<Dependency, DependencyRepository>(
    s3Operations,
    TOOLS_PATH,
    repository,
) {
    override fun findEntity(key: Dependency): Dependency? = repository.findByDemoAndVersionAndFileId(key.demo, key.version, key.fileId)

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @return list of files present in storage for required version
     */
    fun list(
        demo: Demo,
        version: String,
    ): Flux<Dependency> = blockingToFlux {
        repository.findAllByDemo_OrganizationNameAndDemo_ProjectNameAndVersion(
            demo.organizationName,
            demo.projectName,
            version,
        )
    }

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @param fileName name of a file to be deleted
     * @return true if file is successfully deleted, false otherwise
     */
    fun delete(
        demo: Demo,
        version: String,
        fileName: String,
    ): Mono<Unit> = blockingToMono {
        repository.findByDemo_OrganizationNameAndDemo_ProjectNameAndVersionAndFileName(
            demo.organizationName,
            demo.projectName,
            version,
            fileName,
        )
    }
        .flatMap { delete(it) }
        .map {
            log.debug {
                "Deleted $fileName associated with version $version from $demo"
            }
        }

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     * @return true if storage contains some dependency with provided values, otherwise -- false
     */
    fun doesExist(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    ): Mono<Boolean> = findDependency(organizationName, projectName, version, fileName)
        .map { true }
        .defaultIfEmpty(false)

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     * @return [Dependency] found by provided values
     */
    fun findDependency(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    ): Mono<Dependency> = blockingToMono {
        repository.findByDemo_OrganizationNameAndDemo_ProjectNameAndVersionAndFileName(
            organizationName,
            projectName,
            version,
            fileName,
        )
    }

    private fun downloadToTempDir(
        tempDir: Path,
        organizationName: String,
        projectName: String,
        version: String
    ) = blockingToFlux {
        repository.findAllByDemo_OrganizationNameAndDemo_ProjectNameAndVersion(
            organizationName,
            projectName,
            version,
        )
    }
        .map { download(it).collectToFile(tempDir / it.fileName).block() }
        .collectList()

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
    fun archive(
        organizationName: String,
        projectName: String,
        version: String,
        archiveName: String = "archive.zip",
    ): Flux<ByteBuffer> = createTempDirectory().div("archive").createDirectory().let { tempDir ->
        createArchive(tempDir, organizationName, projectName, version, archiveName).doOnComplete {
            tempDir.deleteRecursively()
        }
            .doOnError { tempDir.deleteRecursively() }
    }

    private fun createArchive(
        tmpDir: Path,
        organizationName: String,
        projectName: String,
        version: String,
        archiveName: String = "archive.zip",
    ): Flux<ByteBuffer> =
            downloadToTempDir(tmpDir, organizationName, projectName, version)
                .map {
                    tmpDir.parent.div(archiveName)
                        .also { dirToZip -> tmpDir.compressAsZipTo(dirToZip) }
                }
                .flatMapMany { it.toByteBufferFlux() }

    companion object {
        private val log: Logger = getLogger<DependencyStorage>()
    }
}