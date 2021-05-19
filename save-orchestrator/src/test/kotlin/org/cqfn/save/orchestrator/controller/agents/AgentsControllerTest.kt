package org.cqfn.save.orchestrator.controller.agents

import org.cqfn.save.agent.ExecutionLogs
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.config.Beans
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.controller.AgentsController
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

import java.io.File
import java.nio.charset.Charset
import java.time.LocalDateTime
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@WebFluxTest(controllers = [AgentsController::class])
@Import(AgentService::class, Beans::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentsControllerTest {
    private val stubTime = LocalDateTime.now()

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var configProperties: ConfigProperties
    @MockBean private lateinit var dockerService: DockerService

    @AfterEach
    fun tearDown() {
        mockServer.dispatcher.peek().let { mockResponse ->
            // when `QueueDispatcher.failFast` is true, default value is an empty response with code 404
            val hasDefaultEnqueuedResponse = mockResponse.status == "HTTP/1.1 404 Client Error" && mockResponse.getBody() == null
            Assertions.assertTrue(hasDefaultEnqueuedResponse) {
                "There is an enqueued response in the MockServer after a test has completed. Enqueued body: ${mockResponse.getBody()?.readString(Charset.defaultCharset())}, " +
                        "status: ${mockResponse.status}"
            }
        }
        val pathToLogs = configProperties.agentLogs
        File(pathToLogs).deleteRecursively()
    }

    @Test
    fun `should build image, query backend and start containers`() {
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val execution = Execution(project, stubTime, stubTime, ExecutionStatus.PENDING, "stub", "stub", 0, 20).apply {
            id = 42L
        }
        whenever(dockerService.buildAndCreateContainers(any())).thenReturn(listOf("test-agent-id-1", "test-agent-id-2"))
        // /addAgents
        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(Json.encodeToString(listOf<Long>(1, 2)))
        )
        // /updateAgentStatuses
        mockServer.enqueue(MockResponse().setResponseCode(200))
        // /updateExecution is not mocked, because it's performed by DockerService, and it's mocked in these tests

        webClient
            .post()
            .uri("/initializeAgents")
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk
        Thread.sleep(2_500)  // wait for background task to complete on mocks
        verify(dockerService).buildAndCreateContainers(any())
        verify(dockerService).startContainersAndUpdateExecution(any(), anyList())
    }

    @Test
    fun checkPostResponseIsNotOk() {
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val execution = Execution(project, stubTime, stubTime, ExecutionStatus.RUNNING, "stub", "stub", 0, 20)
        webClient
            .post()
            .uri("/initializeAgents")
            .body(BodyInserters.fromValue(execution))
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
        """.trimIndent().split(System.lineSeparator())
        makeRequestToSaveLog(logs)
            .expectStatus()
            .isOk
        val logFile = File(configProperties.agentLogs + File.separator + "agent.log")
        Assertions.assertTrue(logFile.exists())
        Assertions.assertEquals(logFile.readLines(), logs)
    }

    @Test
    fun `check save log if already exist`() {
        val firstLogs = """
            first line
            second line
        """.trimIndent().split(System.lineSeparator())
        makeRequestToSaveLog(firstLogs)
            .expectStatus()
            .isOk
        val firstLogFile = File(configProperties.agentLogs + File.separator + "agent.log")
        Assertions.assertTrue(firstLogFile.exists())
        Assertions.assertEquals(firstLogFile.readLines(), firstLogs)
        val secondLogs = """
            second line
            first line
        """.trimIndent().split(System.lineSeparator())
        makeRequestToSaveLog(secondLogs)
            .expectStatus()
            .isOk
            .expectStatus()
            .isOk
        val newFirstLogFile = File(configProperties.agentLogs + File.separator + "agent.log")
        Assertions.assertTrue(newFirstLogFile.exists())
        Assertions.assertEquals(newFirstLogFile.readLines(), firstLogs + secondLogs)
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
            createTempDirectory("agentLogs").toAbsolutePath().toString()
        }

        @JvmStatic
        private lateinit var mockServer: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServer.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            // todo: should be initialized in @BeforeAll, but it gets called after @DynamicPropertySource
            mockServer = MockWebServer()
            (mockServer.dispatcher as QueueDispatcher).setFailFast(true)
            mockServer.start()
            registry.add("orchestrator.backendUrl") { "http://localhost:${mockServer.port}" }
            registry.add("orchestrator.agentLogs") { volume }
        }
    }
}
