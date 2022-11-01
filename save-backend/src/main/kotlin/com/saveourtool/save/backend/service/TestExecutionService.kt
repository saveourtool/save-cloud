package com.saveourtool.save.backend.service

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.repository.TestRepository
import com.saveourtool.save.backend.utils.secondsToLocalDateTime
import com.saveourtool.save.core.result.CountWarnings
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Test
import com.saveourtool.save.entities.TestExecution
import com.saveourtool.save.filters.TestExecutionFilters
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.*

import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

import kotlin.io.path.pathString

/**
 * Service for test result
 */
@Service
class TestExecutionService(private val testExecutionRepository: TestExecutionRepository,
                           private val agentRepository: AgentRepository,
                           private val executionRepository: ExecutionRepository,
                           transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param page a zero-based index of page of data
     * @param pageSize size of page
     * @param filters
     * @return a list of [TestExecutionDto]s
     */
    @Suppress("AVOID_NULL_CHECKS", "UnsafeCallOnNullableType")
    internal fun getTestExecutions(
        executionId: Long,
        page: Int,
        pageSize: Int,
        filters: TestExecutionFilters,
    ): List<TestExecution> {
        val wrappedFileName = wrapValue(filters.fileName)
        val wrappedTestSuiteName = wrapValue(filters.testSuite)
        val wrappedTagValue = wrapValue(filters.tag)
        return testExecutionRepository.findByExecutionIdAndStatusAndTestTestSuiteName(
            executionId,
            filters.status,
            wrappedFileName,
            wrappedTestSuiteName,
            wrappedTagValue,
            PageRequest.of(page, pageSize),
        )
    }

    private fun wrapValue(value: String?) = value?.let {
        "%$value%"
    }

    /**
     * Get test executions by [agentContainerId] and [status]
     *
     * @param agentContainerId
     * @param status
     * @return a list of test executions
     */
    internal fun getTestExecutions(agentContainerId: String, status: TestResultStatus) = testExecutionRepository
        .findByAgentContainerIdAndStatus(agentContainerId, status)

    /**
     * @param executionId
     * @return a list of test executions
     */
    internal fun getTestExecutions(executionId: Long): List<TestExecution> =
            testExecutionRepository.findByExecutionId(executionId)

    /**
     * @param executionId
     * @param page
     * @param pageSize
     * @param status
     * @return a list of test executions
     */
    @Suppress("UnsafeCallOnNullableType")
    internal fun getByExecutionIdGroupByTestSuite(
        executionId: Long,
        status: TestResultStatus,
        page: Int,
        pageSize: Int,
    ): List<Array<*>>? =
            testExecutionRepository.findByExecutionIdGroupByTestSuite(executionId, status.name, PageRequest.of(page, pageSize))

    /**
     * Finds TestExecution by test location
     *
     * @param executionId under this executionId test has been executed
     * @param testResultLocation location of the test
     * @return optional TestExecution
     */
    internal fun getTestExecution(executionId: Long, testResultLocation: TestResultLocation) = with(testResultLocation) {
        testExecutionRepository.findByExecutionIdAndTestPluginNameAndTestFilePath(
            executionId, pluginName, FilenameUtils.separatorsToUnix(Paths.get(testLocation, testName).pathString)
        )
    }

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param status
     * @param testSuite
     * @return number of TestExecutions
     */
    @Suppress("AVOID_NULL_CHECKS", "UnsafeCallOnNullableType")
    internal fun getTestExecutionsCount(executionId: Long, status: TestResultStatus?, testSuite: String?) =
            testExecutionRepository.countByExecutionIdAndStatusAndTestTestSuiteName(executionId, status, testSuite)

    /**
     * @param projectId
     */
    internal fun deleteTestExecutionWithProjectId(projectId: Long?) {
        projectId?.let {
            testExecutionRepository.deleteByExecutionProjectId(projectId)
        }
    }

    /**
     * @param executionIds list of ids
     * @return Unit
     */
    internal fun deleteTestExecutionByExecutionIds(executionIds: List<Long>) =
            testExecutionRepository.deleteByExecutionIdIn(executionIds)

    /**
     * @param testExecutionsDtos
     * @return list of lost tests
     */
    @Suppress(
        "TOO_MANY_LINES_IN_LAMBDA",
        "TOO_LONG_FUNCTION",
        "UnsafeCallOnNullableType",
        "LongMethod",
        "MAGIC_NUMBER",
        "MagicNumber",
        "PARAMETER_NAME_IN_OUTER_LAMBDA",
    )
    @Transactional
    fun saveTestResult(testExecutionsDtos: List<TestExecutionDto>): List<TestExecutionDto> {
        log.debug { "Saving ${testExecutionsDtos.size} test results from agent ${testExecutionsDtos.first().agentContainerId}" }
        // we take agent id only from first element, because all test executions have same execution
        val agentContainerId = requireNotNull(testExecutionsDtos.first().agentContainerId) {
            "Attempt to save test results without assigned agent. testExecutionDtos=$testExecutionsDtos"
        }
        val agent = requireNotNull(agentRepository.findByContainerId(agentContainerId)) {
            "Agent with containerId=[$agentContainerId] was not found in the DB"
        }

        val executionId = agent.execution.id!!
        val lostTests: MutableList<TestExecutionDto> = mutableListOf()
        val counters = Counters()
        testExecutionsDtos.forEach { testExecDto ->
            val foundTestExec = testExecutionRepository.findByExecutionIdAndTestPluginNameAndTestFilePath(
                executionId,
                testExecDto.pluginName,
                testExecDto.filePath
            )
            val testExecutionId: Long? = foundTestExec.map { it.id }.orElse(null)
            foundTestExec.also {
                if (it.isEmpty) {
                    log.error("Test execution $testExecDto for execution id=$executionId was not found in the DB")
                }
            }
                .filter {
                    // update only those test executions, that haven't been updated before
                    it.status == TestResultStatus.RUNNING
                }
                .ifPresentOrElse({
                    it.startTime = testExecDto.startTimeSeconds?.secondsToLocalDateTime()
                    it.endTime = testExecDto.endTimeSeconds?.secondsToLocalDateTime()
                    it.status = testExecDto.status
                    when (testExecDto.status) {
                        TestResultStatus.PASSED -> counters.passed++
                        TestResultStatus.FAILED -> counters.failed++
                        else -> counters.skipped++
                    }
                    it.unmatched = testExecDto.unmatched
                    it.matched = testExecDto.matched
                    it.expected = testExecDto.expected
                    it.unexpected = testExecDto.unexpected

                    with(counters) {
                        unmatchedChecks += testExecDto.unmatched.orZeroIfNotApplicable()
                        matchedChecks += testExecDto.matched.orZeroIfNotApplicable()
                        expectedChecks += testExecDto.expected.orZeroIfNotApplicable()
                        unexpectedChecks += testExecDto.unexpected.orZeroIfNotApplicable()
                    }

                    testExecutionRepository.save(it)
                },
                    {
                        lostTests.add(testExecDto)
                        log.error("Test execution $testExecDto with id=$testExecutionId for execution id=$executionId cannot be updated because its status is not RUNNING")
                    })
        }
        transactionTemplate.execute {
            val execution = executionRepository.findWithLockingById(executionId).orElse(null).orNotFound()
            execution.apply {
                log.debug {
                    "Updating counters in execution id=$executionId: running=$runningTests-${counters.total()}, " +
                            "passed=$passedTests+${counters.passed}, failed=$failedTests+${counters.failed}, skipped=$skippedTests+${counters.skipped}"
                }
                runningTests -= counters.total()
                passedTests += counters.passed
                failedTests += counters.failed
                skippedTests += counters.skipped

                unmatchedChecks += counters.unmatchedChecks
                matchedChecks += counters.matchedChecks
                expectedChecks += counters.expectedChecks
                unexpectedChecks += counters.unexpectedChecks

                val executionScore = toDto().calculateScore(scoreType = ScoreType.F_MEASURE)

                if (!executionScore.isValidScore()) {
                    log.error("Execution score for execution id $id is invalid: $executionScore")
                }
                score = executionScore
            }
            executionRepository.save(execution)
        }
        return lostTests
    }

    /**
     * @param execution the [Execution], during which these tests will be executed
     * @param tests the tests, which will be executed
     */
    fun saveTestExecutions(execution: Execution, tests: List<Test>) {
        val executionId = execution.requiredId()
        log.debug { "Will create test executions for executionId=$executionId for tests ${tests.map { it.requiredId() }}" }
        tests.map { test ->
            val testId = test.requiredId()
            val testExecutionList = testExecutionRepository.findByExecutionIdAndTestId(executionId, testId)
            if (testExecutionList.isNotEmpty()) {
                log.debug { "For execution with id=$executionId test id=$testId already exists in DB, deleting it" }
                testExecutionRepository.deleteAllByExecutionIdAndTestId(executionId, testId)
            }
            log.debug("Creating TestExecution for test $testId")
            val testExecution = testExecutionRepository.save(
                TestExecution(
                    test = test,
                    execution = execution,
                    agent = null,
                    status = TestResultStatus.READY_FOR_TESTING,
                    startTime = null,
                    endTime = null,
                    unmatched = null,
                    matched = null,
                    expected = null,
                    unexpected = null,
                )
            )
            log.debug { "Created TestExecution ${testExecution.requiredId()} for test $testId" }
        }
    }

    /**
     * Set `agent` field of test executions corresponding to [testDtos] to [agentContainerId]
     *
     * @param agentContainerId id of an agent
     * @param testDtos test that will be executed by [agentContainerId] agent
     */
    @Transactional
    @Suppress("UnsafeCallOnNullableType")
    fun assignAgentByTest(agentContainerId: String, testDtos: List<TestDto>) {
        val agent = requireNotNull(agentRepository.findByContainerId(agentContainerId)) {
            "Agent with containerId=[$agentContainerId] was not found in the DB"
        }
        val executionId = agent.execution.id!!
        testDtos.forEach { test ->
            val testExecution = testExecutionRepository.findByExecutionIdAndTestPluginNameAndTestFilePath(
                executionId,
                test.pluginName,
                test.filePath
            )
                .orElseThrow {
                    log.error("Can't find test_execution for executionId=$executionId, test.pluginName=${test.pluginName}, test.filePath=${test.filePath}")
                    NoSuchElementException()
                }
            testExecutionRepository.save(testExecution.apply {
                this.agent = agent
            })
        }
    }

    /**
     * @param agentsList the list of agents, for which corresponding test executions should be marked as failed
     * @param condition
     */
    @Transactional
    @Suppress("UnsafeCallOnNullableType")
    fun markTestExecutionsOfAgentsAsFailed(agentsList: Collection<String>, condition: (TestExecution) -> Boolean = { true }) {
        agentsList.forEach { agentContainerId ->
            val agent = requireNotNull(agentRepository.findByContainerId(agentContainerId)) {
                "Agent with containerId=[$agentContainerId] was not found in the DB"
            }
            val agentId = agent.id!!
            val executionId = agent.execution.id!!

            val testExecutionList = testExecutionRepository.findByExecutionIdAndAgentId(
                executionId,
                agentId
            ).filter(condition)

            if (testExecutionList.isEmpty()) {
                // Crashed agent could be not assigned with tests, so just warn and return
                log.warn("Can't find `test_execution`s for executionId=$executionId and agentId=$agentId")
                return@forEach
            }

            testExecutionList.map { testExecution ->
                testExecutionRepository.save(testExecution.apply {
                    this.status = TestResultStatus.INTERNAL_ERROR
                    // In case of execution without errors all information about test execution we take from
                    // json report, however in case when agent is crashed, it's unavailable, so fill at least end time
                    this.endTime = LocalDateTime.now()
                })
            }.also { testExecutions ->
                if (testExecutions.isNotEmpty()) {
                    log.info("Test executions with ids ${testExecutions.map { it.id }} were failed with internal error")
                }
            }
        }
    }

    private fun Long?.orZeroIfNotApplicable() = this?.takeUnless { CountWarnings.isNotApplicable(it.toInt()) } ?: 0

    @Suppress(
        "KDOC_NO_CONSTRUCTOR_PROPERTY",
        "MISSING_KDOC_ON_FUNCTION",
        "LongParameterList",
        "KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT",
    )
    private class Counters(
        var passed: Int = 0,
        var failed: Int = 0,
        var skipped: Int = 0,

        // how many checks/validations are not found, but we expect them
        var unmatchedChecks: Long = 0,
        // how many checks/validations matched to expected results
        var matchedChecks: Long = 0,
        // how many checks/validations we expect
        var expectedChecks: Long = 0,
        // how many checks/validations are found, but we don't expect them
        var unexpectedChecks: Long = 0,

        // note: missedResults = expectedResults - matchedResults
    ) {
        fun total() = passed + failed + skipped
    }

    private companion object {
        private val log: Logger = getLogger<TestExecutionService>()
    }
}
