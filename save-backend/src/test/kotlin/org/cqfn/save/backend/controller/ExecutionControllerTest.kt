package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.execution.ExecutionUpdateDto
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
@ExtendWith(MySqlExtension::class)
class ExecutionControllerTest {
    private val testLocalDateTime = LocalDateTime.of(2020, Month.APRIL, 10, 16, 30, 20)

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var executionRepository: ExecutionRepository

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Test
    fun testConnection() {
        val project = projectRepository.findById(1).get()
        val execution = Execution(
            project,
            testLocalDateTime,
            testLocalDateTime,
            ExecutionStatus.RUNNING,
            "0,1,2",
            "stub",
            0,
            20,
            ExecutionType.MANUAL
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
        val project = projectRepository.findById(1).get()
        val execution = Execution(
            project,
            testLocalDateTime,
            testLocalDateTime,
            ExecutionStatus.RUNNING,
            "0,1,2",
            "stub",
            0,
            20,
            ExecutionType.MANUAL
        )
        webClient.post()
            .uri("/createExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = executionRepository.findAll()

        assertTrue(databaseData.any { it.status == execution.status && it.startTime == testLocalDateTime })
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun testUpdateExecution() {
        val project = projectRepository.findById(1).get()
        val execution = Execution(
            project,
            testLocalDateTime,
            testLocalDateTime,
            ExecutionStatus.RUNNING,
            "0,1,2",
            "stub",
            0,
            20,
            ExecutionType.MANUAL
        )

        webClient.post()
            .uri("/createExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk

        val executionUpdateDto = ExecutionUpdateDto(
            1, ExecutionStatus.FINISHED
        )

        webClient.post()
            .uri("/updateExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionUpdateDto))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = executionRepository.findAll()

        databaseData.forEach {
            println(it.status)
        }

        assertTrue(databaseData.any { it.status == executionUpdateDto.status && it.id == executionUpdateDto.id })
    }

    @Test
    fun checkStatusException() {
        val executionUpdateDto = ExecutionUpdateDto(
            -1, ExecutionStatus.FINISHED
        )
        webClient.post()
            .uri("/updateExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionUpdateDto))
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
