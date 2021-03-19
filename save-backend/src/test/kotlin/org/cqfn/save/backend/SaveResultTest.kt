package org.cqfn.save.backend

import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
class SaveResultTest : DatabaseTestBase() {
    private val date = LocalDateTime.now().withNano(0)

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    @Test
    fun checkSave() {
        val testExecution = TestExecution(
            1,
            1,
            1,
            1,
            1,
            TestResultStatus.FAILED,
            date,
            date)
        webClient.post()
            .uri("/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecution)))
            .exchange().expectBody(String::class.java)
            .isEqualTo<Nothing>("Saved")
        val tests = testExecutionRepository.findAll()
        assertTrue(tests.any { it.startTime == testExecution.startTime.withNano(0) })
        assertTrue(tests.any { it.endTime == testExecution.endTime.withNano(0) })
    }

    @Test
    fun checkErrorRequest() {
        val testExecution = TestExecution(
            2,
            1,
            1,
            1,
            1,
            TestResultStatus.FAILED,
            date,
            date)
        webClient.post()
            .uri("/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecution)))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody(String::class.java)
            .isEqualTo<Nothing>("Some ids don't exist")
        val tests = testExecutionRepository.findAll()
        assertFalse(tests.any { it.startTime == testExecution.startTime.withNano(0) })
        assertFalse(tests.any { it.endTime == testExecution.endTime.withNano(0) })
    }
}
