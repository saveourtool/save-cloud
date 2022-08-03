package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.config.LocalDateTimeConfig
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.service.TestsPreprocessorToBackendBridge
import com.saveourtool.save.preprocessor.utils.RepositoryVolume
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.testutils.*

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono

import java.io.File
import java.time.Duration
import java.time.LocalDateTime
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
    private val standardTestSuitesSourceDto = TestSuitesSourceDto(
        "organizationName",
        "name",
        "description",
        GitDto(
            "some-url",
            null,
            null,
        ),
        "branch",
        "testRootPath",
    )
    @MockBean private lateinit var testDiscoveringService: TestDiscoveringService
    @MockBean private lateinit var testSuitesPreprocessorController: TestSuitesPreprocessorController
    @MockBean private lateinit var testsPreprocessorToBackendBridge: TestsPreprocessorToBackendBridge
    @MockBean private lateinit var webClientCustomizer: WebClientCustomizer

    @BeforeEach
    fun webClientSetUp() {
        webClient = webClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
        whenever(testDiscoveringService.getRootTestConfig(any())).thenReturn(mock())
        whenever(testDiscoveringService.getAllTests(any(), any())).thenReturn(
            sequenceOf(TestDto("foo", "fooPlugin", 15, "86", emptyList()))
        )
    }

    @BeforeEach
    fun setupStandardTestSuitesSource() {
        whenever(testsPreprocessorToBackendBridge.getStandardTestSuitesSources())
            .thenReturn(Mono.just(listOf(standardTestSuitesSourceDto)))
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
        val request = ExecutionRequest(project, wrongRepo, branchOrCommit = null, sdk = Sdk.Default, executionId = execution.id, testRootPath = ".")
        // /updateExecutionByDto
        mockServerBackend.enqueue(
            "/updateExecutionByDto",
            MockResponse().setResponseCode(200)
        )

        webClient.post()
            .uri("/upload")
            .bodyValue(request)
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
        val git = Git(url = validRepo.url, organization = project.organization)
        val testSuitesSource = TestSuitesSource(project.organization, "test", null, git, "main", "examples/kotlin-diktat/")

        whenever(testsPreprocessorToBackendBridge.getOrCreateTestSuitesSource(
            testSuitesSource.organization.name,
            testSuitesSource.git.url,
            testSuitesSource.testRootPath,
            testSuitesSource.branch,
        )).thenReturn(Mono.just(testSuitesSource.toDto()))
        whenever(testSuitesPreprocessorController.fetch(eq(testSuitesSource.toDto()), any()))
            .thenReturn(Mono.just(Unit))
        whenever(testsPreprocessorToBackendBridge.getTestSuites(
            eq(testSuitesSource.organization.name),
            eq(testSuitesSource.name),
            any(),
        )).thenReturn(
            Mono.just(listOf(
                TestSuite("", null, testSuitesSource, "1", LocalDateTime.now())
                    .apply {
                        id = 42L
                    }
            )
            )
        )

        val request = ExecutionRequest(project, validRepo, null, "examples/kotlin-diktat/", Sdk.Default, execution.id)
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
            yield(mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS))
        }.onEach {
            logger.info("Request $it")
        }
        webClient.post()
            .uri("/upload")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody<String>()
            .isEqualTo(executionResponseBody(executionId))
        Thread.sleep(15_000)
        assertions.forEach { Assertions.assertNotNull(it) }
    }

    @Suppress("LongMethod")
    @Test
    fun testSaveProjectAsBinaryFile() {
        val version1 = TestSuitesSourceSnapshotKey(
            "organizationName",
            "testSuitesSourceName",
            "version-1",
            1L
        )

        val version2Value = "version-2"
        val version2 = TestSuitesSourceSnapshotKey(
            "organizationName",
            "testSuitesSourceName",
            version2Value,
            2L
        )

        whenever(testsPreprocessorToBackendBridge.listTestSuitesSourceVersions(standardTestSuitesSourceDto))
            .thenReturn(Mono.just(listOf(version2, version1)))

        val git: Git = mock()
        whenever(git.url).thenReturn("some-url")
        val testSuitesSource: TestSuitesSource = mock()
        whenever(testSuitesSource.git).thenReturn(git)

        val testSuite1: TestSuite = mock()
        whenever(testSuite1.name).thenReturn("Chapter1")
        whenever(testSuite1.source).thenReturn(testSuitesSource)
        whenever(testSuite1.version).thenReturn(version2Value)
        val testSuite2: TestSuite = mock()

        whenever(testSuite2.name).thenReturn("Chapter2")
        whenever(testSuite2.source).thenReturn(testSuitesSource)
        whenever(testSuite2.version).thenReturn(version2Value)

        whenever(testsPreprocessorToBackendBridge.getTestSuites(any(), any(), eq(version2Value)))
            .thenReturn(Mono.just(listOf(testSuite1, testSuite2)))

        val project = Project.stub(42)
        val executionId = 98L
        val execution = Execution.stub(project).apply {
            testSuiteIds = "1"
            type = ExecutionType.STANDARD
            id = executionId
        }
        val request = ExecutionRequestForStandardSuites(project, listOf("Chapter1"), Sdk.Default, null, null, executionId)

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
        whenever(testsPreprocessorToBackendBridge.getStandardTestSuitesSources())
            .thenReturn(Mono.just(listOf(standardTestSuitesSourceDto)))
        whenever(testSuitesPreprocessorController.fetch(standardTestSuitesSourceDto))
            .thenReturn(Mono.just(Unit))

        webClient.post()
            .uri("/uploadStandardTestSuite")
            .exchange()
            .expectStatus()
            .isAccepted
        verify(testsPreprocessorToBackendBridge).getStandardTestSuitesSources()
        verifyNoMoreInteractions(testsPreprocessorToBackendBridge)
        verify(testSuitesPreprocessorController).fetch(standardTestSuitesSourceDto)
        verifyNoMoreInteractions(testSuitesPreprocessorController)
    }

    @Test
    @Suppress("LongMethod")
    fun `rerun execution type git`() {
        val project = Project.stub(42)
        val execution = Execution.stub(project).apply {
            id = 98L
        }

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
            .uri("/rerunExecution?id=${execution.requiredId()}")
            .contentType(MediaType.APPLICATION_JSON)
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
            .uri("/rerunExecution?id=${execution.requiredId()}")
            .contentType(MediaType.APPLICATION_JSON)
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
