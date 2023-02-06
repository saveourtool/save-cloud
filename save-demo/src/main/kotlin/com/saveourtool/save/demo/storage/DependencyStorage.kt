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
    ): Flux<Dependency> = blockingToFlux { repository.findAllByDemoAndVersion(demo, version) }

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

    companion object {
        private val log: Logger = getLogger<DependencyStorage>()
    }
}
