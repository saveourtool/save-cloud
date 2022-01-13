package org.cqfn.save.backend.repository

import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param status test status
     * @param name suite name
     * @return number of TestExecutions
     */
    @Query(
        """SELECT COUNT(te) FROM TestExecution te
           JOIN Test t ON t.id = te.test
           JOIN Execution e ON e.id = te.execution
           JOIN TestSuite ts ON t.testSuite = ts.id
           WHERE 1 = 1
            and (:status is null or te.status = :status)
            and (:name is null or ts.name = :name)
            and e.id = :executionId"""
    )
    fun countByExecutionIdAndStatusAndTestTestSuiteName(
        @Param("executionId") executionId: Long,
        @Param("status") status: TestResultStatus?,
        @Param("name") name: String?,
    ): Int

    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param status test status
     * @param name suite name
     * @param pageable a request for a page
     * @return a list of [TestExecutionDto]s
     */
    @Query(
        """SELECT te FROM TestExecution te
           JOIN Test t ON t.id = te.test
           JOIN Execution e ON e.id = te.execution
           JOIN TestSuite ts ON t.testSuite = ts.id
           WHERE 1 = 1
            and (:status is null or te.status = :status)
            and (:name is null or ts.name = :name)
            and e.id = :executionId"""
    )
    fun findByExecutionIdAndStatusAndTestTestSuiteName(
        @Param("executionId") executionId: Long,
        @Param("status") status: TestResultStatus?,
        @Param("name") name: String?,
        pageable: Pageable
    ): List<TestExecution>

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
