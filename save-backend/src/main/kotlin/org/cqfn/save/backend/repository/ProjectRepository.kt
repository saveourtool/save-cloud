package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
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
}
