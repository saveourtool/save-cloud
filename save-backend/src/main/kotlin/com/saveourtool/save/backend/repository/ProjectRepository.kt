package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.ProjectStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

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
    fun findByNameAndOrganizationName(name: String, organizationName: String): Project?

    /**
     * @param organizationName
     * @return list of projects for organization
     */
    fun findByOrganizationName(organizationName: String): List<Project>


    /**
     * @param prefix prefix of organization name
     * @param status
     * @return list of organizations with names that start with [prefix]
     */
    fun findByNameStartingWithAndStatusIn(prefix: String, status: List<ProjectStatus>): List<Project>


    /**
     * @param status
     * @return list of organizations with required status
     */
    fun findByStatusIn(status: List<ProjectStatus>): List<Project>
}
