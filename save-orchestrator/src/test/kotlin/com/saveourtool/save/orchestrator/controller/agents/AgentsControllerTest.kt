package com.saveourtool.save.orchestrator.controller.agents

import com.saveourtool.save.agent.ExecutionLogs
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.Beans
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.orchestrator.docker.AgentRunner
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

import java.io.File

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@WebFluxTest(controllers = [AgentsController::class])
@Import(AgentService::class, Beans::class)
@MockBeans(MockBean(AgentRunner::class))
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentsControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var configProperties: ConfigProperties
    @MockBean private lateinit var dockerService: DockerService

    @AfterEach
    fun tearDown() {
        val pathToLogs = configProperties.executionLogs
        File(pathToLogs).deleteRecursively()
    }

    @Test
    fun `should build image, query backend and start containers`() {
        val project = Project.stub(null)
        val execution = Execution.stub(project).apply {
            status = ExecutionStatus.PENDING
            testSuiteIds = "1"
            id = 42L
        }
        whenever(dockerService.buildBaseImage(any(), any())).thenReturn("test-image-id" to "test-exec-cmd")
        whenever(dockerService.createContainers(any(), any())).thenReturn(listOf("test-agent-id-1", "test-agent-id-2"))
        // /addAgents
        mockServer.enqueue(
            "/addAgents.*",
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(Json.encodeToString(listOf<Long>(1, 2)))
        )
        // /updateAgentStatuses
        mockServer.enqueue("/updateAgentStatuses", MockResponse().setResponseCode(200))
        // /updateExecutionByDto is not mocked, because it's performed by DockerService, and it's mocked in these tests

        val bodyBuilder = MultipartBodyBuilder().apply {
            part("execution", execution)
        }.build()

        webClient
            .post()
            .uri("/initializeAgents")
            .body(BodyInserters.fromMultipartData(bodyBuilder))
            .exchange()
            .expectStatus()
            .isAccepted
        Thread.sleep(2_500)  // wait for background task to complete on mocks
        verify(dockerService).buildBaseImage(any(), any())
        verify(dockerService).createContainers(any(), any())
        verify(dockerService).startContainersAndUpdateExecution(any(), anyList())
    }

    @Test
    fun checkPostResponseIsNotOk() {
        val project = Project.stub(null)
        val execution = Execution.stub(project)
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("execution", execution)
        }.build()

        webClient
            .post()
            .uri("/initializeAgents")
            .body(BodyInserters.fromMultipartData(bodyBuilder))
            .exchange()
            .expectStatus()
            .is4xxClientError
    }

    @Test
    fun `should stop agents by id`() {
        webClient
            .post()
            .uri("/stopAgents")
            .body(BodyInserters.fromValue(listOf("id-of-agent")))
            .exchange()
            .expectStatus()
            .isOk
        verify(dockerService).stopAgents(anyList())
    }

    @Test
    fun `should save logs`() {
        val logs = """
            first line
            second line
        """.trimIndent().lines()
        makeRequestToSaveLog(logs)
            .expectStatus()
            .isOk
        val logFile = File(configProperties.executionLogs + File.separator + "agent.log")
        Assertions.assertTrue(logFile.exists())
        Assertions.assertEquals(logFile.readLines(), logs)
    }

    @Test
    fun `check save log if already exist`() {
        val firstLogs = """
            first line
            second line
        """.trimIndent().lines()
        makeRequestToSaveLog(firstLogs)
            .expectStatus()
            .isOk
        val firstLogFile = File(configProperties.executionLogs + File.separator + "agent.log")
        Assertions.assertTrue(firstLogFile.exists())
        Assertions.assertEquals(firstLogFile.readLines(), firstLogs)

        val secondLogs = """
            second line
            first line
        """.trimIndent().lines()
        makeRequestToSaveLog(secondLogs)
            .expectStatus()
            .isOk
            .expectStatus()
            .isOk
        val newFirstLogFile = File(configProperties.executionLogs + File.separator + "agent.log")
        Assertions.assertTrue(newFirstLogFile.exists())
        Assertions.assertEquals(newFirstLogFile.readLines(), firstLogs + secondLogs)
    }

    @Test
    fun `should cleanup execution artifacts`() {
        mockServer.enqueue(
            "/getAgentsIdsForExecution.*",
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(Json.encodeToString(listOf("container-1", "container-2", "container-3")))
        )

        webClient.post()
            .uri("/cleanup?executionId=42")
            .exchange()
            .expectStatus()
            .isOk

        Thread.sleep(2_500)
        verify(dockerService, times(1)).cleanup(anyLong())
        verify(dockerService, times(1)).removeImage(anyString())
    }

    private fun makeRequestToSaveLog(text: List<String>): WebTestClient.ResponseSpec {
        val executionLogs = ExecutionLogs("agent", text)
        return webClient
            .post()
            .uri("/executionLogs")
            .body(BodyInserters.fromValue(executionLogs))
            .exchange()
    }

    companion object {
        @OptIn(ExperimentalPathApi::class)
        private val volume: String by lazy {
            createTempDirectory("executionLogs").toAbsolutePath().toString()
        }

        @JvmStatic
        private lateinit var mockServer: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServer.checkQueues()
            mockServer.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServer.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            // todo: should be initialized in @BeforeAll, but it gets called after @DynamicPropertySource
            mockServer = createMockWebServer()
            mockServer.start()
            registry.add("orchestrator.backendUrl") { "http://localhost:${mockServer.port}" }
            registry.add("orchestrator.executionLogs") { volume }
        }
    }
}
