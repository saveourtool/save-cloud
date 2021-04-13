package org.cqfn.save.backend

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.repository.BaseEntityRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.backend.utils.toLocalDateTime
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution

import org.junit.jupiter.api.Assertions.assertFalse
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
class SaveResultTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var testExecutionRepository: BaseEntityRepository<TestExecution>

    @Test
    fun `should save TestExecutionDto into the DB`() {
        val testExecutionDto = TestExecutionDto(
            1,
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
        assertTrue(tests.any { it.startTime == testExecutionDto.startTime.toLocalDateTime().withNano(0) })
        assertTrue(tests.any { it.endTime == testExecutionDto.endTime.toLocalDateTime().withNano(0) })
    }

    @Test
    fun checkErrorRequest() {
        val testExecutionDto = TestExecutionDto(
            999,
            1,
            TestResultStatus.FAILED,
            DEFAULT_DATE_TEST_EXECUTION,
            DEFAULT_DATE_TEST_EXECUTION)
        webClient.post()
            .uri("/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDto)))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody<String>()
            .isEqualTo("Some ids don't exist")
        val tests = testExecutionRepository.findAll()
        assertFalse(tests.any { it.startTime == testExecutionDto.startTime.toLocalDateTime().withNano(0) })
        assertFalse(tests.any { it.endTime == testExecutionDto.endTime.toLocalDateTime().withNano(0) })
    }

    companion object {
        private const val DEFAULT_DATE_TEST_EXECUTION = 1L
    }
}
