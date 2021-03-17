package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
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
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var executionRepository: ExecutionRepository

    private val testLocalDateTime = LocalDateTime.of(2020, Month.APRIL, 10, 16, 30, 20)

    @Test
    fun testConnection() {
        val execution = Execution(
            0,
                testLocalDateTime,
                testLocalDateTime,
                ExecutionStatus.RUNNING,
                "0,1,2",
                0
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
                "0,1,2",
                1
        )
        webClient.post()
                .uri("/createExecution")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(execution))
                .exchange()
                .expectStatus()
                .isOk

        val databaseData = executionRepository.findAll()

        assert(databaseData.any { it.status == execution.status && it.id == execution.id })
    }
}