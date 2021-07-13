package org.cqfn.save.backend.service

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.utils.toLocalDateTime
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Service for test result
 */
@Service
class TestExecutionService(private val testExecutionRepository: TestExecutionRepository) {
    private val log = LoggerFactory.getLogger(TestExecutionService::class.java)

    @Autowired
    private lateinit var testRepository: TestRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository

    @Autowired
    private lateinit var executionRepository: ExecutionRepository

    /**
     * Returns a page of [TestExecution]s with [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @param page a zero-based index of page of data
     * @param pageSize size of page
     * @param pageSize
     * @return a list of [TestExecutionDto]s
     */
    internal fun getTestExecutions(executionId: Long, page: Int, pageSize: Int) = testExecutionRepository
        .findByExecutionId(executionId, PageRequest.of(page, pageSize))

    /**
     * Returns number of TestExecutions with this [executionId]
     *
     * @param executionId an ID of Execution to group TestExecutions
     * @return number of TestExecutions
     */
    internal fun getTestExecutionsCount(executionId: Long) = testExecutionRepository
        .countByExecutionId(executionId)

    /**
     * @param testExecutionsDtos
     * @return list of lost tests
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "UnsafeCallOnNullableType")
    fun saveTestResult(testExecutionsDtos: List<TestExecutionDto>): List<TestExecutionDto> {
        // we take agent id only from first element, because all test executions have same execution
        val agentContainerId = requireNotNull(testExecutionsDtos.first().agentContainerId) {
            "Attempt to save test results without assigned agent. testExecutionDtos=$testExecutionsDtos"
        }
        val agent = requireNotNull(agentRepository.findByContainerId(agentContainerId)) {
            "Agent with containerId=[$agentContainerId] was not found in the DB"
        }
        val execution = agent.execution
        val lostTests: MutableList<TestExecutionDto> = mutableListOf()
        testExecutionsDtos.forEach { testExecDto ->
            val foundTestExec = testExecutionRepository.findByExecutionIdAndAgentIdAndTestFilePath(execution.id!!, agent.id!!, testExecDto.filePath)
            foundTestExec.ifPresentOrElse({
                it.startTime = testExecDto.startTimeSeconds?.toLocalDateTime()
                it.endTime = testExecDto.endTimeSeconds?.toLocalDateTime()
                it.status = testExecDto.status
                when (testExecDto.status) {
                    TestResultStatus.PASSED -> execution.passedTests += 1
                    TestResultStatus.FAILED -> execution.failedTests += 1
                    else -> execution.skippedTests += 1
                }
                testExecutionRepository.save(it)
            },
                {
                    lostTests.add(testExecDto)
                    log.error("Test execution $testExecDto was not found in the DB")
                })
        }
        executionRepository.save(execution)
        return lostTests
    }

    /**
     * @param testIds IDs of the tests, which will be executed
     * @param executionId ID of the [Execution], during which these tests will be executed
     */
    fun saveTestExecution(executionId: Long, testIds: List<Long>) {
        log.debug("Will create test executions for executionId=$executionId for tests $testIds")
        testIds.map { testId ->
            testRepository.findById(testId).ifPresentOrElse({ test ->
                testExecutionRepository.save(
                    TestExecution(test,
                        executionId,
                        null, TestResultStatus.READY, null, null)
                )
            },
                { log.error("Can't find test with id = $testId to save in testExecution") }
            )
        }
    }
}
