package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.config.LocalDateTimeConfig
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.utils.RepositoryVolume
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteType
import com.saveourtool.save.testutils.*

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@WebFluxTest(controllers = [DownloadProjectController::class])
@Import(LocalDateTimeConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient(timeout = "60000")
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
class DownloadProjectTest(
    @Autowired private var webClient: WebTestClient,
    @Autowired private val configProperties: ConfigProperties,
    @Autowired private val objectMapper: ObjectMapper
) : RepositoryVolume {
    private val binFolder = "${configProperties.repository}/binFolder"
    private val binFilePath = "$binFolder/program"
    private val propertyPath = "$binFolder/save.properties"
    @MockBean private lateinit var testDiscoveringService: TestDiscoveringService

    @BeforeEach
    fun webClientSetUp() {
        webClient = webClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
        whenever(testDiscoveringService.getRootTestConfig(any())).thenReturn(mock())
        whenever(testDiscoveringService.getAllTests(any(), any())).thenReturn(
            sequenceOf(TestDto("foo", "fooPlugin", 15, "86", emptyList()))
        )
    }

    @BeforeAll
    fun setUp() {
        File(binFolder).mkdirs()
    }

    @Test
    fun testBadRequest() {
        val organization: Organization = Organization("Huawei", OrganizationStatus.CREATED, 1, null).apply {
            id = 1
        }
        val project = Project("owner", "someName", "wrongGit", ProjectStatus.CREATED, userId = 2, organization = organization)
        val wrongRepo = GitDto("wrongGit")
        val execution = Execution.stub(project).apply {
            id = 97L
        }
        val request = ExecutionRequest(project, wrongRepo, sdk = Sdk.Default, executionId = execution.id, testRootPath = ".")
        // /updateExecutionByDto
        mockServerBackend.enqueue(
            "/updateExecutionByDto",
            MockResponse().setResponseCode(200)
        )

        val multipart = MultipartBodyBuilder().apply {
            part("executionRequest", request)
        }
            .build()
        webClient.post()
            .uri("/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipart))
            .exchange()
            .expectStatus()
            .isAccepted
        Thread.sleep(15_000)  // Time for request to delete directory
        Assertions.assertFalse(File("${configProperties.repository}/${wrongRepo.url.hashCode()}").exists())
    }

    /**
     * This one covers logic of connecting to services
     */
    @Test
    fun testCorrectDownload() {
        val project = Project.stub(42).apply {
            url = "https://github.com/saveourtool/save-cli.git"
        }
        val executionId = 99L
        val execution = Execution.stub(project).apply {
            id = executionId
        }
        val validRepo = GitDto("https://github.com/saveourtool/save-cli.git")
        val request = ExecutionRequest(project, validRepo, "examples/kotlin-diktat/", Sdk.Default, execution.id)
        // /saveTestSuites
        mockServerBackend.enqueue(
            "/saveTestSuites",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(
                    listOf(
                        TestSuite(TestSuiteType.PROJECT, "", null, project, LocalDateTime.now(), "save.properties", "https://github.com/saveourtool/save-cli.git").apply {
                            id = 42L
                        }
                    )
                )),
        )

        // /initializeTests
        mockServerBackend.enqueue(
            "/initializeTests",
            MockResponse()
                .setResponseCode(200)
        )
        // /updateNewExecution
        mockServerBackend.enqueue(
            "/updateNewExecution",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(execution))
        )
        // /executeTestsByExecutionId?executionId
        mockServerBackend.enqueue(
            "/executeTestsByExecutionId\\?executionId=(\\d)+",
            MockResponse()
                .setResponseCode(200)
        )
        // /initializeAgents
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = sequence {
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            logger.info("Request $it")
        }
        val multipart = MultipartBodyBuilder().apply {
            part("executionRequest", request)
        }
            .build()
        webClient.post()
            .uri("/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multipart))
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody<String>()
            .isEqualTo(executionResponseBody(executionId))
        Thread.sleep(15_000)
        
        val dirName = listOf(validRepo.url).hashCode()
        Assertions.assertTrue(File("${configProperties.repository}/$dirName").exists())
        assertions.forEach { Assertions.assertNotNull(it) }
    }

    @Suppress("LongMethod")
    @Test
    fun testSaveProjectAsBinaryFile() {
        val project = Project.stub(42)
        val executionId = 98L
        val execution = Execution.stub(project).apply {
            testSuiteIds = "1"
            type = ExecutionType.STANDARD
            id = executionId
        }
        val request = ExecutionRequestForStandardSuites(project, listOf("Chapter1"), Sdk.Default, null, null, executionId, "version")

        // /test-suites/standard/ids-by-name
        mockServerBackend.enqueue(
            "/test-suites/standard/ids-by-name",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(
                    listOf(
                        42
                    )
                )),
        )

        // /updateNewExecution
        mockServerBackend.enqueue(
            "/updateNewExecution",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(execution))
        )

        // /executeTestsByExecutionId
        mockServerBackend.enqueue(
            "/executeTestsByExecutionId\\?executionId=(\\d)+",
            MockResponse()
                .setResponseCode(200)
        )

        // /initializeAgents
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(200)
        )

        val assertions = sequence {
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            logger.info("Request $it")
        }

        webClient.post()
            .uri("/uploadBin")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody<String>()
            .isEqualTo(executionResponseBody(executionId))
        Thread.sleep(15_000)

        assertions.forEach { Assertions.assertNotNull(it) }
    }

    @Test
    fun testStandardTestSuites() {
        val requestSize = readStandardTestSuitesFile(configProperties.reposFileName)
            .toList()
            .flatMap { it.testSuitePaths }
            .size
        repeat(requestSize) {
            val project = Project.stub(42)

            val tempDir = "${configProperties.repository}/${"https://github.com/saveourtool/save-cli".hashCode()}/examples/kotlin-diktat/"
            val config = "${tempDir}save.toml"
            File(tempDir).mkdirs()
            File(config).createNewFile()
            whenever(testDiscoveringService.getRootTestConfig(any())).thenReturn(
                TestConfig(
                    config.toPath(),
                    null,
                    mutableListOf(),
                    FileSystem.SYSTEM
                )
            )

            mockServerBackend.enqueue(
                "/saveTestSuites",
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        objectMapper.writeValueAsString(
                            listOf(
                                TestSuite(TestSuiteType.PROJECT, "", null, project, LocalDateTime.now(), "save.properties")
                            )
                        )
                    ),
            )
        }

        repeat(requestSize) {
            mockServerBackend.enqueue(
                "/initializeTests",
                MockResponse()
                    .setResponseCode(200)
            )
        }

        // /allStandardTestSuites
        mockServerBackend.enqueue(
            "/allStandardTestSuites",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(
                    listOf(
                        TestSuiteDto(TestSuiteType.STANDARD, "stub", null, null, "save.properties", "stub")
                    )
                ))
        )

        // /deleteTestSuite
        mockServerBackend.enqueue(
            "/markObsoleteTestSuites",
            MockResponse()
                .setResponseCode(200)
        )

        val assertions = MutableList(requestSize * 2) {
            CompletableFuture.supplyAsync {
                mockServerBackend.takeRequest(60, TimeUnit.SECONDS)
            }
        }.also { list ->
            list.add(CompletableFuture.supplyAsync { mockServerBackend.takeRequest(60, TimeUnit.SECONDS) })
            list.add(CompletableFuture.supplyAsync { mockServerBackend.takeRequest(60, TimeUnit.SECONDS) })
        }

        webClient.post()
            .uri("/uploadStandardTestSuite")
            .exchange()
            .expectStatus()
            .isAccepted
        Thread.sleep(15_000)
        assertions.map { it.orTimeout(60, TimeUnit.SECONDS).join() }
            .forEach { Assertions.assertNotNull(it) }
        Assertions.assertTrue(File("${configProperties.repository}/${"https://github.com/saveourtool/save-cli".hashCode()}").exists())
    }

    @Test
    @Suppress("LongMethod")
    fun `rerun execution type git`() {
        val project = Project.stub(42)
        val execution = Execution.stub(project).apply {
            id = 98L
        }
        val request = ExecutionRequest(project, GitDto("https://github.com/saveourtool/save-cli"), "examples/kotlin-diktat/", Sdk.Default, execution.id)

        // /updateExecutionByDto
        mockServerBackend.enqueue(
            "/updateExecutionByDto",
            MockResponse().setResponseCode(200)
        )
        // /cleanup
        mockServerOrchestrator.enqueue(
            "/cleanup\\?executionId=(\\d)+",
            MockResponse()
                .setResponseCode(200)
        )
        // /execution
        mockServerBackend.enqueue(
            "/execution\\?id=(\\d)+",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(execution))
        )
        // /executeTestsByExecutionId?executionId=$executionId
        mockServerBackend.enqueue(
            "/executeTestsByExecutionId\\?executionId=(\\d)+",
            MockResponse()
                .setResponseCode(200)
        )
        // /initializeAgents
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = sequence {
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            logger.info("Request $it")
        }

        webClient.post()
            .uri("/rerunExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody<String>()
            .isEqualTo("Clone pending")
        Thread.sleep(30_000)

        assertions.forEach { Assertions.assertNotNull(it) }
    }

    @Test
    @Suppress("LongMethod")
    fun `rerun execution type standard`() {
        val project = Project.stub(42)
        val execution = Execution.stub(project).apply {
            testSuiteIds = "1"
            type = ExecutionType.STANDARD
            id = 98L
            status = ExecutionStatus.PENDING
        }
        val request = ExecutionRequest(project, GitDto("https://github.com/saveourtool/save-cli"), "examples/kotlin-diktat/", Sdk.Default, execution.id)

        // /updateExecutionByDto
        mockServerBackend.enqueue("/updateExecutionByDto", MockResponse().setResponseCode(200))

        // /cleanup
        mockServerOrchestrator.enqueue("/cleanup\\?executionId=(\\d)+", MockResponse().setResponseCode(200))

        // /execution
        mockServerBackend.enqueue(
            "/execution\\?id=(\\d)+",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(execution))
        )

        // /executeTestsByExecutionId
        mockServerBackend.enqueue(
            "/executeTestsByExecutionId\\?executionId=(\\d)+",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(execution))
        )

        // /initializeAgents
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = sequence {
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerBackend.takeRequest(60, TimeUnit.SECONDS))
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }
            .onEach {
                logger.info("Request $it")
            }

        webClient.post()
            .uri("/rerunExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody<String>()
            .isEqualTo("Clone pending")
        Thread.sleep(15_000)

        assertions.forEach { Assertions.assertNotNull(it) }
    }

    @AfterEach
    fun removeBinDir() {
        mockServerBackend.checkQueues()
        mockServerOrchestrator.checkQueues()
        File(configProperties.repository).deleteRecursively()
        File(binFolder).deleteRecursively()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadProjectTest::class.java)

        @JvmStatic
        lateinit var mockServerBackend: MockWebServer

        @JvmStatic
        lateinit var mockServerOrchestrator: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerBackend.checkQueues()
            mockServerBackend.cleanup()
            mockServerOrchestrator.checkQueues()
            mockServerOrchestrator.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerBackend.shutdown()
            mockServerOrchestrator.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerBackend = createMockWebServer()
            mockServerBackend.start()
            mockServerOrchestrator = createMockWebServer()
            mockServerOrchestrator.start()
            registry.add("save.backend") { "http://localhost:${mockServerBackend.port}" }
            registry.add("save.orchestrator") { "http://localhost:${mockServerOrchestrator.port}" }
        }
    }
}
