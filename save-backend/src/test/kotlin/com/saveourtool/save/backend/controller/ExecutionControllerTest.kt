package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.ProjectRepository
import com.saveourtool.save.backend.repository.TestSuiteRepository
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionInitializationDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.testsuite.TestSuiteType
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.save.v1

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.springframework.security.test.context.support.WithMockUser
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
    MockBean(ProjectController::class),
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
    lateinit var testSuiteRepository: TestSuiteRepository

    @Test
    @WithMockUser("John Doe")
    fun testDataSave() {
        val project = projectRepository.findById(1).get()
        val execution = Execution.stub(project).apply {
            startTime = testLocalDateTime
            endTime = testLocalDateTime
        }
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
    @WithMockUser("John Doe")
    @Suppress("TOO_LONG_FUNCTION")
    fun testUpdateExecution() {
        val project = projectRepository.findById(1).get()
        val execution = Execution.stub(project)

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
    @WithMockUser
    fun checkExecutionDto() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }

        webClient.get()
            .uri("/api/$v1/executionDto?executionId=1")
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
    @WithMockUser
    fun checkExecutionDtoByProject() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 99)
        }

        val project = projectRepository.findById(1).get()
        val executionCounts = executionRepository.findAll().count { it.project.id == project.id }
        webClient.get()
            .uri("/api/$v1/executionDtoList?name=${project.name}&organizationName=${project.organization.name}")
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
    @WithMockUser("John Doe")
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun checkUpdateNewExecution() {
        val execution = Execution.stub(projectRepository.findAll().first())
        webClient.post()
            .uri("/internal/createExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk

        val executionUpdate = ExecutionInitializationDto(execution.project, "1, 2, 3", "testPath", "executionVersion", null, null)
        webClient.post()
            .uri("/internal/updateNewExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionUpdate))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<Execution>()
            .consumeWith {
                val responseBody = requireNotNull(it.responseBody)
                assertEquals("1, 2, 3", responseBody.testSuiteIds)
                assertEquals("testPath", responseBody.resourcesRootPath)
                assertEquals(20, responseBody.batchSize)
                assertEquals("executionVersion", responseBody.version)
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
    @WithMockUser(username = "John Doe")
    fun `should send request to preprocessor to rerun execution`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 2)
        }

        mockServerPreprocessor.enqueue(
            "/rerunExecution.*",
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
            .uri("/api/$v1/rerunExecution?id=2")
            .exchange()
            .expectStatus()
            .isOk
        assertions.orTimeout(60, TimeUnit.SECONDS).join().forEach {
            assertNotNull(it)
        }
    }

    @Suppress("TOO_LONG_FUNCTION")
    @Test
    @WithMockUser(username = "John Doe")
    fun `test testSuiteIds`() {
        val project = projectRepository.findById(1).get()
        val executionEmptyTestSuiteIds = Execution.stub(project).apply {
            testSuiteIds = null
        }

        webClient.post()
            .uri("/internal/findTestRootPathForExecutionByTestSuites")
            .body(BodyInserters.fromValue(executionEmptyTestSuiteIds))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<String>>()
            .consumeWith {
                val responseBody = requireNotNull(it.responseBody)
                assertTrue(responseBody.isEmpty())
            }

        val testSuite1 = testSuiteRepository.save(
            TestSuite(
                type = TestSuiteType.PROJECT,
                project = project,
                dateAdded = testLocalDateTime,
                testRootPath = "test 1",
            )
        )
        val testSuite2 = testSuiteRepository.save(
            TestSuite(
                type = TestSuiteType.PROJECT,
                project = project,
                dateAdded = testLocalDateTime,
                testRootPath = "test 2",
            )
        )
        val testSuite3 = testSuiteRepository.save(
            TestSuite(
                type = TestSuiteType.PROJECT,
                project = project,
                dateAdded = testLocalDateTime,
                testRootPath = "test 3",
            )
        )
        val executionTestSuiteIds = Execution.stub(project).apply {
            testSuiteIds = "${testSuite1.id}, ${testSuite2.id}, ${testSuite3.id}"
        }

        webClient.post()
            .uri("/internal/findTestRootPathForExecutionByTestSuites")
            .body(BodyInserters.fromValue(executionTestSuiteIds))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<String>>()
            .consumeWith {
                val responseBody = requireNotNull(it.responseBody)
                assertFalse(responseBody.isEmpty())
                assertEquals(listOf("test 1", "test 2", "test 3"), responseBody)
            }
    }

    companion object {
        @JvmStatic lateinit var mockServerPreprocessor: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerPreprocessor.checkQueues()
            mockServerPreprocessor.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = createMockWebServer()
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
