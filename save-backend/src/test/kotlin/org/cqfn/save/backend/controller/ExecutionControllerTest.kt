package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionUpdateDto
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime
import java.time.Month

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
class ExecutionControllerTest : DatabaseTestBase() {
    private val testLocalDateTime = LocalDateTime.of(2020, Month.APRIL, 10, 16, 30, 20)

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var executionRepository: ExecutionRepository

    @Test
    fun testConnection() {
        val execution = Execution(
            0,
            testLocalDateTime,
            testLocalDateTime,
            ExecutionStatus.RUNNING,
            "0,1,2"
        )
        webClient.post()
            .uri("/createExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun testDataSave() {
        val execution = Execution(
            1,
            testLocalDateTime,
            testLocalDateTime,
            ExecutionStatus.RUNNING,
            "0,1,2"
        )
        webClient.post()
            .uri("/createExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = executionRepository.findAll()

        assertTrue(databaseData.any { it.status == execution.status && it.id == execution.id })
    }

    @Test
    fun testUpdateExecution() {
        val execution = Execution(
                1,
                testLocalDateTime,
                testLocalDateTime,
                ExecutionStatus.RUNNING,
                "0,1,2"
        )

        webClient.post()
                .uri("/createExecution")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(execution))
                .exchange()
                .expectStatus()
                .isOk

        val executionUpdateDto = ExecutionUpdateDto(
                0, ExecutionStatus.FINISHED
        )

        webClient.post()
                .uri("/updateExecution")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(executionUpdateDto))
                .exchange()
                .expectStatus()
                .isOk

        val databaseData = executionRepository.findAll()

        assertTrue(databaseData.any { it.status == executionUpdateDto.status && it.id == executionUpdateDto.id })
    }
}
