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

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @return number of TestExecutions
     */
    fun countByExecutionId(executionId: Long): Int

    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param pageable a request for a page
     * @return a list of [TestExecutionDto]s
     */
    fun findByExecutionId(executionId: Long, pageable: Pageable): List<TestExecution>
}
