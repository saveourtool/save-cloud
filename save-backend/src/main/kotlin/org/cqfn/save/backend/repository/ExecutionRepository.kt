package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Repository
import java.util.Optional

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
    fun findTopByProjectNameAndProjectOwnerOrderByStartTimeDesc(name: String, owner: String): Optional<Execution>

    /**
     * @param project to find execution
     * @return execution
     */
    fun findTopByProjectOrderByStartTimeDesc(project: Project): Execution?
}
