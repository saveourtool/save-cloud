package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.controllers.ExecutionController
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.domain.Sdk
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionInitializationDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.execution.ExecutionUpdateDto

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.time.LocalDateTime
import java.time.Month
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
)
class ExecutionControllerTest {
    private val testLocalDateTime = LocalDateTime.of(2020, Month.APRIL, 10, 16, 30, 20)

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var executionRepository: ExecutionRepository

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var executionController: ExecutionController

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
            20,
            ExecutionType.GIT,
            "0.0.1",
            0,
            0,
            0,
            0,
            Sdk.Default.toString(),
            null,
            execCmd = "",
            batchSizeForAnalyzer = ""
        )
        webClient.post()
            .uri("/internal/createExecution")
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
            20,
            ExecutionType.GIT,
            "0.0.1",
            0,
            0,
            0,
            0,
            Sdk.Default.toString(),
            null,
            execCmd = "",
            batchSizeForAnalyzer = ""
        )
        webClient.post()
            .uri("/internal/createExecution")
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
            20,
            ExecutionType.GIT,
            "0.0.1",
            0,
            0,
            0,
            0,
            Sdk.Default.toString(),
            null,
            execCmd = "",
            batchSizeForAnalyzer = ""
        )

        webClient.post()
            .uri("/internal/createExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk

        val executionUpdateDto = ExecutionUpdateDto(
            1, ExecutionStatus.FINISHED
        )

        webClient.post()
            .uri("/internal/updateExecutionByDto")
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
            .uri("/internal/updateExecutionByDto")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionUpdateDto))
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun checkExecutionDto() {
        webClient.get()
            .uri("/api/executionDto?executionId=1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ExecutionDto>()
            .consumeWith {
                requireNotNull(it.responseBody)
                assertEquals(ExecutionType.GIT, it.responseBody!!.type)
            }
    }

    @Test
    fun checkExecutionDtoByProject() {
        val project = projectRepository.findById(1).get()
        val executionCounts = executionRepository.findAll().count { it.project == project }
        webClient.get()
            .uri("/api/executionDtoList?name=${project.name}&owner=${project.owner}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<ExecutionDto>>()
            .consumeWith {
                requireNotNull(it.responseBody)
                assertEquals(executionCounts, it.responseBody!!.size)
            }
    }

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun checkUpdateNewExecution() {
        val execution = Execution(projectRepository.findAll().first(), LocalDateTime.now(), null, ExecutionStatus.PENDING, null,
            null, 20, ExecutionType.GIT, null, 0, 0, 0, 0, Sdk.Default.toString(), null, execCmd = "", batchSizeForAnalyzer = "")
        webClient.post()
            .uri("/internal/createExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk

        val executionUpdate = ExecutionInitializationDto(execution.project, "1, 2, 3", "testPath", "executionVersion", execCmd = "", batchSizeForAnalyzer = "")
        webClient.post()
            .uri("/internal/updateNewExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionUpdate))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<Execution>()
            .consumeWith {
                requireNotNull(it.responseBody)
                assertEquals("1, 2, 3", it.responseBody.testSuiteIds)
                assertEquals("testPath", it.responseBody.resourcesRootPath)
                assertEquals(20, it.responseBody.batchSize)
                assertEquals("executionVersion", it.responseBody.version)
            }
        val isUpdatedExecution = executionRepository.findAll().any {
            it.testSuiteIds == "1, 2, 3" &&
                    it.resourcesRootPath == "testPath" &&
                    it.batchSize == 20 &&
                    it.version == "executionVersion"
        }
        assertTrue(isUpdatedExecution)
    }

    @Test
    fun `should send request to preprocessor to rerun execution`() {
        mockServerPreprocessor.enqueue(
            MockResponse().setResponseCode(202)
                .setHeader("Accept", "application/json")
                .setHeader("Content-Type", "application/json")
                .setBody("Clone pending")
        )
        val assertions = CompletableFuture.supplyAsync {
            listOf(
                mockServerPreprocessor.takeRequest(60, TimeUnit.SECONDS),
            )
        }

        webClient.post()
            .uri("/api/rerunExecution?id=2")
            .exchange()
            .expectStatus()
            .isOk
        assertions.orTimeout(60, TimeUnit.SECONDS).join().forEach {
            assertNotNull(it)
        }
    }

    companion object {
        @JvmStatic lateinit var mockServerPreprocessor: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = MockWebServer()
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
