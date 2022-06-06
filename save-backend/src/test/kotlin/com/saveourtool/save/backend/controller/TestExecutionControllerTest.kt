package com.saveourtool.save.backend.controller

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.agent.TestSuiteExecutionStatisticDto
import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.backend.utils.secondsToLocalDateTime
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.v1

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(ProjectController::class),
)
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
    @WithMockUser
    fun `should count TestExecutions for a particular Execution`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }

        webClient.get()
            .uri("/api/$v1/testExecution/count?executionId=1")
            .exchange()
            .expectBody<Int>()
            .isEqualTo(28)
    }

    @Test
    @WithMockUser
    fun `should return a page of TestExecutions for a particular Execution`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }

        webClient.get()
            .uri("/api/$v1/testExecutions?executionId=1&page=0&size=20")
            .exchange()
            .expectBody<List<TestExecutionDto>>()
            .consumeWith {
                Assertions.assertEquals(20, it.responseBody!!.size)
            }
    }

    @Test
    @WithMockUser
    fun `should return a list test suits with number of test for executions id`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }

        webClient.get()
            .uri("/api/$v1/testLatestExecutions?executionId=3&status=${TestResultStatus.PASSED}&page=0&size=10")
            .exchange()
            .expectBody<List<TestSuiteExecutionStatisticDto>>()
            .consumeWith {
                Assertions.assertEquals(1, it.responseBody!!.size)
            }
    }

    /**
     * Don't forget: SpringBootTest with controllers doesn't rollback transactions like test with repositories only.
     * Should be using different execution id for newly inserted data in order not to clash with tests
     * that check data read.
     */
    @Test
    @WithMockUser
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun `should save TestExecutionDto into the DB`() {
        val testExecutionDtoFirst = TestExecutionDto(
            "testPath63",
            "WarnPlugin",
            "container-3",
            TestResultStatus.FAILED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION,
            missingWarnings = 3,
            matchedWarnings = 2,
        )
        val testExecutionDtoSecond = TestExecutionDto(
            "testPath42",
            "WarnPlugin",
            "container-3",
            TestResultStatus.PASSED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION,
            missingWarnings = 4,
            matchedWarnings = 3,
        )
        val passedTestsBefore = getExecutionsTestsResultByAgentContainerId(testExecutionDtoSecond.agentContainerId!!, true)
        val failedTestsBefore = getExecutionsTestsResultByAgentContainerId(testExecutionDtoFirst.agentContainerId!!, false)
        webClient.post()
            .uri("/internal/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDtoFirst, testExecutionDtoSecond)))
            .exchange()
            .expectBody<String>()
            .isEqualTo("Saved")
        val tests = getAllTestExecutions()
        val passedTestsAfter = getExecutionsTestsResultByAgentContainerId(testExecutionDtoSecond.agentContainerId!!, true)
        val failedTestsAfter = getExecutionsTestsResultByAgentContainerId(testExecutionDtoFirst.agentContainerId!!, false)
        assertTrue(tests.any { it.startTime == testExecutionDtoFirst.startTimeSeconds!!.secondsToLocalDateTime().withNano(0) })
        assertTrue(tests.any { it.endTime == testExecutionDtoFirst.endTimeSeconds!!.secondsToLocalDateTime().withNano(0) })
        Assertions.assertEquals(passedTestsBefore, passedTestsAfter - 1)
        Assertions.assertEquals(failedTestsBefore, failedTestsAfter - 1)
        assertTrue(tests.any { it.missingWarnings == testExecutionDtoFirst.missingWarnings && it.matchedWarnings == testExecutionDtoFirst.matchedWarnings })
        assertTrue(tests.any { it.missingWarnings == testExecutionDtoSecond.missingWarnings && it.matchedWarnings == testExecutionDtoSecond.matchedWarnings })
    }

    @Test
    @WithMockUser
    @Suppress("UnsafeCallOnNullableType")
    fun `should not save data if provided fields are invalid`() {
        val testExecutionDto = TestExecutionDto(
            "test-not-exists",
            "WarnPlugin",
            "container-1",
            TestResultStatus.FAILED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION,
            missingWarnings = null,
            matchedWarnings = null,
        )
        webClient.post()
            .uri("/internal/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDto)))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody<String>()
            .isEqualTo("Some ids don't exist or cannot be updated")
        val testExecutions = testExecutionRepository.findAll()
        assertTrue(testExecutions.none { it.test.filePath == "test-not-exists" })
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun getAllTestExecutions() =
            transactionTemplate.execute {
                testExecutionRepository.findAll()
            }!!

    @Suppress("UnsafeCallOnNullableType")
    private fun getExecutionsTestsResultByAgentContainerId(id: String, isPassed: Boolean) =
            transactionTemplate.execute {
                if (isPassed) {
                    agentRepository.findByContainerId(id)!!.execution.passedTests
                } else {
                    agentRepository.findByContainerId(id)!!.execution.failedTests
                }
            }!!

    companion object {
        private const val DEFAULT_DATE_TEST_EXECUTION = 1L
    }
}
