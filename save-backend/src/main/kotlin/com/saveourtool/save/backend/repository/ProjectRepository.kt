package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * The repository of project entities
 */
@Repository
interface ProjectRepository : JpaRepository<Project, Long>, QueryByExampleExecutor<Project>,
JpaSpecificationExecutor<Project> {
    /**
     * @param name
     * @param organization
     * @return project by name and owner
     */
    fun findByNameAndOrganization(name: String, organization: Organization): Project?

    /**
     * @param name
     * @param organizationName
     * @return project by name and owner
     */
    fun findByNameAndOrganizationName(name: String, organizationName: String): Optional<Project>

    /**
     * @param organizationName
     * @return list of projects for organization
     */
    fun findByOrganizationName(organizationName: String): List<Project>
}
