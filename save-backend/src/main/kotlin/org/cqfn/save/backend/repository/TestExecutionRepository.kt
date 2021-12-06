package org.cqfn.save.backend.repository

import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import javax.transaction.Transactional

/**
 * Repository of execution
 */
@Repository
interface TestExecutionRepository : BaseEntityRepository<TestExecution>, JpaSpecificationExecutor<TestExecution> {
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

    @Suppress("MISSING_KDOC_ON_FUNCTION", "MISSING_KDOC_CLASS_ELEMENTS")
    fun countByExecutionIdAndStatus(executionId: Long, status: TestResultStatus): Int

    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param pageable a request for a page
     * @return a list of [TestExecutionDto]s
     */
    fun findByExecutionId(executionId: Long, pageable: Pageable): List<TestExecution>

    @Suppress("MISSING_KDOC_ON_FUNCTION", "MISSING_KDOC_CLASS_ELEMENTS")
    fun findByExecutionIdAndStatus(executionId: Long, status: TestResultStatus, pageable: Pageable): List<TestExecution>

    /**
     * Returns test executions for agent [agentContainerId] and status [status]
     *
     * @param agentContainerId
     * @param status
     * @return a list of test executions
     */
    fun findByAgentContainerIdAndStatus(agentContainerId: String, status: TestResultStatus): List<TestExecution>

    /**
     * Returns a TestExecution matched by a set of fields
     *
     * @param executionId if of execution
     * @param filePath path to test file
     * @param pluginName name of the plugin from test execution
     * @return Optional TestExecution
     */
    fun findByExecutionIdAndTestPluginNameAndTestFilePath(executionId: Long, pluginName: String, filePath: String): Optional<TestExecution>

    /**
     * Returns a TestExecution matched by a set of fields
     *
     * @param executionId id of execution
     * @param testId id of test
     * @return list of TestExecution's
     */
    fun findByExecutionIdAndTestId(executionId: Long, testId: Long): List<TestExecution>

    /**
     * Delete a TestExecution matched by a set of fields
     *
     * @param executionId id of execution
     * @param testId id of test
     */
    @Transactional
    fun deleteAllByExecutionIdAndTestId(executionId: Long, testId: Long)

    /** Returns a TestExecution matched by the test id
     *
     * @param testId test id
     * @return Optional TestExecution
     */
    fun findByTestId(testId: Long): Optional<TestExecution>

    /**
     * Delete a TestExecution with execution Ids
     *
     * @param executionIds list ids of execution
     */
    @Transactional
    fun deleteByExecutionIdIn(executionIds: List<Long>)

    /**
     * Delete a TestExecution with execution Ids
     *
     * @param id project id
     */
    @Transactional
    fun deleteByExecutionProjectId(id: Long)
}
