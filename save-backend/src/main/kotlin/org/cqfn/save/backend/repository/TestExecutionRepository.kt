package org.cqfn.save.backend.repository

import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.util.Optional

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

    /** Returns a TestExecution matched by the test id
     *
     * @param testId test id
     * @return Optional TestExecution
     */
    fun findByTestId(testId: Long): Optional<TestExecution>
}
