package org.cqfn.save.backend.service

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.utils.secondsToLocalDateTime
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestExecution
import org.cqfn.save.test.TestDto

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import javax.persistence.criteria.Root
import javax.persistence.criteria.Subquery

import kotlin.io.path.pathString

/**
 * Service for test result
 */
@Service
class TestExecutionService(private val testExecutionRepository: TestExecutionRepository,
                           private val testRepository: TestRepository,
                           private val agentRepository: AgentRepository,
                           private val executionRepository: ExecutionRepository,
                           transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(TestExecutionService::class.java)
    private val locks: ConcurrentHashMap<Long, Any> = ConcurrentHashMap()
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param page a zero-based index of page of data
     * @param pageSize size of page
     * @return a list of [TestExecutionDto]s
     */
    internal fun getTestExecutions(executionId: Long, page: Int, pageSize: Int) = testExecutionRepository
        .findByExecutionId(executionId, PageRequest.of(page, pageSize))

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
     * Finds TestExecution by test location
     *
     * @param executionId under this executionId test has been executed
     * @param testResultLocation location of the test
     * @return optional TestExecution
     */
    internal fun getTestExecution(executionId: Long, testResultLocation: TestResultLocation) = with(testResultLocation) {
        testExecutionRepository.findByExecutionIdAndTestPluginNameAndTestFilePath(
            executionId, pluginName, Paths.get(testLocation, testName).pathString
        )
    }

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @return number of TestExecutions
     */
    internal fun getTestExecutionsCount(executionId: Long) = testExecutionRepository
        .countByExecutionId(executionId)

    /**
     * @param projectId
     */
    internal fun deleteTestExecutionWithProjectId(projectId: Long?) {
        val testExecutions = testExecutionRepository.findAll { root, cq, cb ->
            val executionSq: Subquery<Long> = cq.subquery(Long::class.java)
            val executionRoot: Root<Execution> = executionSq.from(Execution::class.java)
            executionSq.select(executionRoot.get("id"))
                .where(
                    cb.equal(executionRoot.get<Project>("project").get<Long>("id"), projectId)
                )
            cb.`in`(root.get<Long>("executionId")).value(executionSq)
        }
        testExecutions.forEach {
            testExecutionRepository.delete(it)
        }
    }

    /**
     * @param executionIds list of ids
     * @return Unite
     */
    internal fun deleteTestExecutionByExecutionIds(executionIds: List<Long>) =
            testExecutionRepository.deleteByExecutionIdIn(executionIds)

    /**
     * @param testExecutionsDtos
     * @return list of lost tests
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "TOO_LONG_FUNCTION", "UnsafeCallOnNullableType")
    @Transactional
    fun saveTestResult(testExecutionsDtos: List<TestExecutionDto>): List<TestExecutionDto> {
        log.debug("Saving ${testExecutionsDtos.size} test results from agent ${testExecutionsDtos.first().agentContainerId}")
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
                    testExecutionRepository.save(it)
                },
                    {
                        lostTests.add(testExecDto)
                        log.error("Test execution $testExecDto for execution id=$executionId cannot be updated because its status is not RUNNING")
                    })
        }
        val lock = locks.computeIfAbsent(executionId) { Any() }
        synchronized(lock) {
            transactionTemplate.execute {
                val execution = executionRepository.findById(executionId).get()
                execution.apply {
                    log.debug("Updating counters in execution id=$executionId: running=$runningTests-${counters.total()}, " +
                            "passed=$passedTests+${counters.passed}, failed=$failedTests+${counters.failed}, skipped=$skippedTests+${counters.skipped}"
                    )
                    runningTests -= counters.total()
                    passedTests += counters.passed
                    failedTests += counters.failed
                    skippedTests += counters.skipped
                }
                executionRepository.save(execution)
            }
        }
        return lostTests
    }

    /**
     * @param testIds IDs of the tests, which will be executed
     * @param executionId ID of the [Execution], during which these tests will be executed
     */
    fun saveTestExecution(executionId: Long, testIds: List<Long>) {
        log.debug("Will create test executions for executionId=$executionId for tests $testIds")
        testIds.map { testId ->
            val testExecutionList = testExecutionRepository.findByExecutionIdAndTestId(executionId, testId)
            if (testExecutionList.isNotEmpty()) {
                log.debug("For execution with id=$executionId test id=$testId already exist in DB, deleting it")
                testExecutionRepository.deleteAllByExecutionIdAndTestId(executionId, testId)
            }
            testRepository.findById(testId).ifPresentOrElse({ test ->
                log.debug("Creating TestExecution for test $testId")
                val id = testExecutionRepository.save(
                    TestExecution(test,
                        executionId,
                        null, TestResultStatus.READY, null, null,
                    )
                )
                log.debug("Created TestExecution $id for test $testId")
            },
                { log.error("Can't find test with id = $testId to save in testExecution") }
            )
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

    @Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY", "MISSING_KDOC_ON_FUNCTION")
    private class Counters(
        var passed: Int = 0,
        var failed: Int = 0,
        var skipped: Int = 0,
    ) {
        fun total() = passed + failed + skipped
    }
}
