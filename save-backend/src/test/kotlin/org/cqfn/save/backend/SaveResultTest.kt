package org.cqfn.save.backend

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.backend.utils.toLocalTimeDate
import org.cqfn.save.domain.TestResultStatus

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
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class SaveResultTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    @Test
    fun checkSave() {
        val testExecutionDto = TestExecutionDto(
            1,
            1,
            TestResultStatus.FAILED,
            date,
            date)
        webClient.post()
            .uri("/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDto)))
            .exchange().expectBody(String::class.java)
            .isEqualTo<Nothing>("Saved")
        val tests = testExecutionRepository.findAll()
        assertTrue(tests.any { it.startTime == testExecutionDto.startTime.toLocalTimeDate().withNano(0) })
        assertTrue(tests.any { it.endTime == testExecutionDto.endTime.toLocalTimeDate().withNano(0) })
    }

    @Test
    fun checkErrorRequest() {
        val testExecutionDto = TestExecutionDto(
            2,
            1,
            TestResultStatus.FAILED,
            date,
            date)
        webClient.post()
            .uri("/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecutionDto)))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody(String::class.java)
            .isEqualTo<Nothing>("Some ids don't exist")
        val tests = testExecutionRepository.findAll()
        assertFalse(tests.any { it.startTime == testExecutionDto.startTime.toLocalTimeDate().withNano(0) })
        assertFalse(tests.any { it.endTime == testExecutionDto.endTime.toLocalTimeDate().withNano(0) })
    }

    companion object {
        private val date = 1L
    }
}
