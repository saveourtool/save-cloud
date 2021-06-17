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
     * @param project
     * @return list of executions
     */
    fun getAllByProject(project: Project): List<Execution>?
}
