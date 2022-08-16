package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.v1

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
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

    @Autowired
    lateinit var testSuitesSourceRepository: TestSuitesSourceRepository

    @Autowired
    lateinit var gitRepository: GitRepository

    @Test
    @WithMockUser("JohnDoe")
    @Suppress("TOO_LONG_FUNCTION")
    fun testUpdateExecution() {
        val executionUpdateDto = ExecutionUpdateDto(1, ExecutionStatus.FINISHED)

        webClient.post()
            .uri("/internal/updateExecutionByDto")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(executionUpdateDto))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = executionRepository.findAll()

        databaseData.forEach {
            log.debug { "${it.status}" }
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
    @WithMockUser("JohnDoe")
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
            .uri("/api/$v1/rerunExecution?id=5")
            .exchange()
            .expectStatus()
            .isOk
        assertions.orTimeout(60, TimeUnit.SECONDS).join().forEach {
            assertNotNull(it)
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    @Test
    @WithMockUser(username = "admin")
    fun `test testSuiteIds`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }
        val execution = executionRepository.findById(6).get()
        val executionEmptyTestSuiteIds = executionRepository.save(execution.apply {
            testSuiteIds = null
        })

        webClient.get()
            .uri("/api/$v1/getTestRootPathByExecutionId?id={id}", executionEmptyTestSuiteIds.requiredId())
            .exchange()
            .expectStatus()
            .isNotFound

        val organization = execution.project.organization
        val git = gitRepository.save(
            Git(
                url = "test",
                username = null,
                password = null,
                organization = organization,
            )
        )
        val source1 = testSuitesSourceRepository.save(
            TestSuitesSource(
                organization = organization,
                name = "test1",
                description = null,
                git = git,
                branch = "main",
                testRootPath = "testRootPath"
            )
        )
        val testSuite1 = testSuiteRepository.save(
            TestSuite(
                name = "test1",
                description = null,
                source = source1,
                version = "1",
                dateAdded = testLocalDateTime,
            )
        )
        val testSuite2 = testSuiteRepository.save(
            TestSuite(
                name = "test2",
                description = null,
                source = source1,
                version = "1",
                dateAdded = testLocalDateTime,
            )
        )

        val validExecutionTestSuiteIds = executionRepository.save(execution.apply {
            formatAndSetTestSuiteIds(listOf(testSuite1.requiredId(), testSuite2.requiredId()))
        })
        webClient.get()
            .uri("/api/$v1/getTestRootPathByExecutionId?id={id}", validExecutionTestSuiteIds.requiredId())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<String>()
            .consumeWith {
                val responseBody = requireNotNull(it.responseBody)
                assertEquals("testRootPath", responseBody)
            }

        val testSuite3 = testSuiteRepository.save(
            TestSuite(
                name = "test3",
                description = null,
                source = testSuitesSourceRepository.save(
                    TestSuitesSource(
                        organization = organization,
                        name = "test3",
                        description = null,
                        git = git,
                        branch = "main",
                        testRootPath = "anotherTestRootPath"
                    )
                ),
                version = "1",
                dateAdded = testLocalDateTime,
            )
        )
        val invalidExecutionTestSuiteIds = executionRepository.save(execution.apply {
            formatAndSetTestSuiteIds(listOf(testSuite1.requiredId(), testSuite2.requiredId(), testSuite3.requiredId()))
        })

        webClient.get()
            .uri("/api/$v1/getTestRootPathByExecutionId?id={id}", invalidExecutionTestSuiteIds.requiredId())
            .exchange()
            .expectStatus()
            .isNotFound
    }

    companion object {
        private val log: Logger = getLogger<ExecutionControllerTest>()
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
