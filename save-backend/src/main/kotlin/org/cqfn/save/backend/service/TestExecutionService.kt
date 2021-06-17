package org.cqfn.save.backend.service

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.utils.toLocalDateTime
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
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
    private lateinit var testSuiteRepository: TestSuiteRepository

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
    fun saveTestResult(testExecutionsDtos: List<TestExecutionDto>): List<TestExecutionDto> {
        val lostTests: MutableList<TestExecutionDto> = mutableListOf()
        val execution = agentRepository
            .findByIdOrNull(testExecutionsDtos.first().agentId) // we take agent id only from first element, because all test executions have same execution
            ?.execution
            ?: run {
                log.error("Agent with id=[${testExecutionsDtos.first().agentId}] was not found in the DB")
                return lostTests
            }
        testExecutionsDtos.forEach { testExecDto ->
            val foundTestExec = testExecutionRepository.findById(testExecDto.id)
            foundTestExec.ifPresentOrElse({
                it.run {
                    this.startTime = testExecDto.startTimeSeconds?.toLocalDateTime()
                    this.endTime = testExecDto.endTimeSeconds?.toLocalDateTime()
                    this.status = testExecDto.status
                    when(testExecDto.status) {
                        TestResultStatus.PASSED -> execution.passedTests += 1
                        TestResultStatus.FAILED -> execution.failedTests += 1
                        TestResultStatus.IGNORED -> execution.skippedTests += 1
                    }
                    testExecutionRepository.save(this)
                }
            },
                {
                    lostTests.add(testExecDto)
                    log.error("Test execution with id=[${testExecDto.id}] was not found in the DB")
                })
        }
        executionRepository.save(execution)
        return lostTests
    }

    /**
     * @param testsId IDs of the tests, which will be executed
     * @param executionId ID of the [Execution], during which these tests will be executed
     */
    fun saveTestExecution(executionId: Long, testsId: List<Long>) {
        testsId.map { testId ->
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
