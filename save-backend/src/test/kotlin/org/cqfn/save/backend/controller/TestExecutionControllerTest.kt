package org.cqfn.save.backend.controller

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.backend.utils.toLocalDateTime
import org.cqfn.save.domain.TestResultStatus

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class TestExecutionControllerTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @BeforeEach
    fun setUp() {
        transactionTemplate = TransactionTemplate(transactionManager)
    }

    @Test
    fun `should count TestExecutions for a particular Execution`() {
        webClient.get()
            .uri("/testExecutionsCount?executionId=1")
            .exchange()
            .expectBody<Int>()
            .isEqualTo(28)
    }

    @Test
    fun `should return a page of TestExecutions for a particular Execution`() {
        webClient.get()
            .uri("/testExecutions?executionId=1&page=0&size=20")
            .exchange()
            .expectBody<List<TestExecutionDto>>()
            .consumeWith {
                Assertions.assertEquals(20, it.responseBody.size)
            }
    }

    /**
     * Don't forget: SpringBootTest with controllers doesn't rollback transactions like test with repositories only.
     * Should be using different execution id for newly inserted data in order not to clash with tests
     * that check data read.
     */
    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun `should save TestExecutionDto into the DB`() {
        val testExecutionDtoFirst = TestExecutionDto(
            1,
            "testFilePath",
            1,
            TestResultStatus.FAILED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION
        )
        val testExecutionDtoSecond = TestExecutionDto(
            2,
            "testFilePath",
            1,
            TestResultStatus.PASSED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION
        )
        val passedTestsBefore = getExecutionsTestsResultByAgentId(testExecutionDtoSecond.agentId!!, true)
        val failedTestsBefore = getExecutionsTestsResultByAgentId(testExecutionDtoFirst.agentId!!, false)
        webClient.post()
            .uri("/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDtoFirst, testExecutionDtoSecond)))
            .exchange()
            .expectBody<String>()
            .isEqualTo("Saved")
        val tests = getAllTestExecutions()
        val passedTestsAfter = getExecutionsTestsResultByAgentId(testExecutionDtoSecond.agentId!!, true)
        val failedTestsAfter = getExecutionsTestsResultByAgentId(testExecutionDtoFirst.agentId!!, false)
        assertTrue(tests.any { it.startTime == testExecutionDtoFirst.startTimeSeconds!!.toLocalDateTime().withNano(0) })
        assertTrue(tests.any { it.endTime == testExecutionDtoFirst.endTimeSeconds!!.toLocalDateTime().withNano(0) })
        Assertions.assertEquals(passedTestsBefore, passedTestsAfter - 1)
        Assertions.assertEquals(failedTestsBefore, failedTestsAfter - 1)
    }

    @Test
    fun `should not save data if provided IDs are invalid`() {
        val invalidId = 999L
        val testExecutionDto = TestExecutionDto(
            invalidId,
            "testFilePath",
            1,
            TestResultStatus.FAILED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION
        )
        webClient.post()
            .uri("/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDto)))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody<String>()
            .isEqualTo("Some ids don't exist")
        val testExecutions = testExecutionRepository.findAll()
        assertTrue(testExecutions.none { it.id == invalidId })
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun getAllTestExecutions() =
            transactionTemplate.execute {
                testExecutionRepository.findAll()
            }!!

    @Suppress("UnsafeCallOnNullableType")
    private fun getExecutionsTestsResultByAgentId(id: Long, isPassed: Boolean) =
            transactionTemplate.execute {
                if (isPassed) {
                    agentRepository.findByIdOrNull(id)!!.execution.passedTests
                } else {
                    agentRepository.findByIdOrNull(id)!!.execution.failedTests
                }
            }!!

    companion object {
        private const val DEFAULT_DATE_TEST_EXECUTION = 1L
    }
}
