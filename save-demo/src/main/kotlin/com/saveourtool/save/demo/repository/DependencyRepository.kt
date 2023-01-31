package com.saveourtool.save.demo.repository

import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [Dependency] entity.
 */
@Repository
interface DependencyRepository : BaseEntityRepository<Dependency> {
    /**
     * @param demo
     * @param version
     * @return list of [Dependency] for [demo]'s specific [version] run
     */
    fun findAllByDemoAndVersion(demo: Demo, version: String): List<Dependency>

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @return list of [Dependency] for [organizationName]/[projectName] demo's specific [version] run
     */
    fun findAllByDemoOrganizationNameAndDemoProjectNameAndVersion(
        organizationName: String,
        projectName: String,
        version: String,
    ): List<Dependency>

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     */
    @Suppress("IDENTIFIER_LENGTH")
    fun deleteByDemoOrganizationNameAndDemoProjectNameAndVersionAndFileName(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    )
}
