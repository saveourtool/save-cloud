package org.cqfn.save.backend.repository

import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Repository of execution
 */
@Repository
interface TestExecutionRepository : BaseEntityRepository<TestExecution> {
    /**
     * @param status
     * @param id
     * @param pageable
     * @return list of test executions
     */
    fun findByStatusAndExecutionId(status: TestResultStatus, id: Long, pageable: Pageable): List<TestExecution>
}
