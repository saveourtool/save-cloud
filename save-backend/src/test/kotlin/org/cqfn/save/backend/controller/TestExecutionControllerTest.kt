package org.cqfn.save.backend.controller

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.backend.utils.toLocalDateTime
import org.cqfn.save.domain.TestResultStatus

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class TestExecutionControllerTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

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
        val testExecutionDto = TestExecutionDto(
            2,
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
            .expectBody<String>()
            .isEqualTo("Saved")
        val tests = testExecutionRepository.findAll()
        assertTrue(tests.any { it.startTime == testExecutionDto.startTimeSeconds!!.toLocalDateTime().withNano(0) })
        assertTrue(tests.any { it.endTime == testExecutionDto.endTimeSeconds!!.toLocalDateTime().withNano(0) })
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

    companion object {
        private const val DEFAULT_DATE_TEST_EXECUTION = 1L
    }
}
