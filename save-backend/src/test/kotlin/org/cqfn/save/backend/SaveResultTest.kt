package org.cqfn.save.backend

import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.TestExecution

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
class SaveResultTest : DatabaseTestBase() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    @Test
    fun checkSave() {
        val date = LocalDateTime.now().withNano(0)
        val testExecution = TestExecution(
            1,
            1,
            1,
            1,
            1,
            TestResultStatus.PASSED,
            date,
            date)
        val beforePost = testExecutionRepository.findAll()
        assertTrue(beforePost.any { it.status == TestResultStatus.FAILED })
        webClient.post()
            .uri("/saveTestResult")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testExecution)))
            .exchange().expectBody(String::class.java)
            .isEqualTo<Nothing>("Save")
        val tests = testExecutionRepository.findAll()
        assertTrue(tests.any { it.startTime == testExecution.startTime.withNano(0) })
        assertTrue(tests.any { it.endTime == testExecution.endTime.withNano(0) })
    }
}
