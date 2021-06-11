package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

/**
 * The repository of project entities
 */
@Repository
interface ProjectRepository : JpaRepository<Project, Long>, QueryByExampleExecutor<Project> {
    /**
     * @param name
     * @param owner
     * @return project by name and owner
     */
    fun findByNameAndOwner(name: String, owner: String): Project
}
