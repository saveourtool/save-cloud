package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Repository

/**
 * Repository of execution
 */
@Repository
interface ExecutionRepository : BaseEntityRepository<Execution> {
    /**
     * @param name name of project
     * @param owner owner of project
     * @return list of executions
     */
    fun getAllByProjectNameAndProjectOwner(name: String, owner: String): List<Execution>

    /**
     * Get latest (by start time an) execution by project name and project owner
     *
     * @param name name of project
     * @param owner owner of project
     * @return execution or null if it was not found
     */
    fun findTopByProjectNameAndProjectOwnerOrderByStartTimeDesc(name: String, owner: String): Execution?

    fun findTopByProjectOrderByStartTimeDesc(project: Project): Execution?
}
