package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Execution
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
}
