package org.cqfn.save.orchestrator.controller.heartbeat

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.ExecutionProgress
import org.cqfn.save.agent.Heartbeat
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.entities.AgentStatusDto
import org.cqfn.save.orchestrator.config.Beans
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService
import org.cqfn.save.test.TestBatchDto

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono

import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDateTime

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@WebFluxTest
@Import(Beans::class, AgentService::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HeartbeatControllerTest {
    @Autowired lateinit var webClient: WebTestClient
    @Autowired private lateinit var agentService: AgentService
    @MockBean private lateinit var dockerService: DockerService
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun webClientSetUp() {
        webClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
    }

    @AfterEach
    fun tearDown() {
        mockServer.dispatcher.peek().let { mockResponse ->
            assertTrue(mockResponse.getBody().let { it == null || it.size == 0L }) {
                "There is an enqueued response in the MockServer after a test has completed. Enqueued body: ${mockResponse.getBody()?.readString(Charset.defaultCharset())}"
            }
        }
    }

    @Test
    fun checkAcceptingHeartbeat() {
        val heartBeatBusy = Heartbeat("test", AgentState.BUSY, ExecutionProgress(0))

        webClient.post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(Mono.just(heartBeatBusy), Heartbeat::class.java)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun checkNewJobResponse() {
        val list = listOf(TestBatchDto("qwe", 0, 1))
        mockServer.enqueue(
            MockResponse()
                .setBody(Json.encodeToString(list))
                .addHeader("Content-Type", "application/json")
        )

        val monoResponse = agentService.setNewTestsIds().block() as NewJobResponse

        assertTrue(monoResponse.ids.isNotEmpty() && monoResponse.ids.first().filePath == "qwe")
    }

    @Test
    fun `should not shutdown any agents when not all of them are IDLE`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "test-2"),
            ),
            heartbeat = Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100)),
            tests = emptyList(),
            mockAgentStatuses = true,
        ) {
            verify(dockerService, times(0)).stopAgents(any())
        }
    }

    @Test
    fun `should not shutdown any agents when all agents are IDLE but there are more tests left`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
            ),
            heartbeat = Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100)),
            tests = listOf(
                TestBatchDto("/path/to/test-1", 1, 1),
                TestBatchDto("/path/to/test-2", 1, 2),
                TestBatchDto("/path/to/test-3", 1, 3),
            ),
            mockAgentStatuses = false,
        ) {
            verify(dockerService, times(0)).stopAgents(any())
        }
    }

    @Test
    fun `should shutdown idle agents when there are no tests left`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
            ),
            heartbeat = Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100)),
            tests = emptyList(),
            mockAgentStatuses = true,
        ) {
            verify(dockerService, times(1)).stopAgents(any())
        }
    }

    /**
     * Test logic triggered by a heartbeat.
     *
     * @param agentStatuses agent statuses that are returned from backend (mocked response)
     * @param heartbeat a [Heartbeat] that is received by orchestrator
     * @param tests a batch of tests returned from backend (mocked response)
     * @param mockAgentStatuses whether a mocked response for `/getAgentsStatusesForSameExecution` should be added to queue
     * @param verification a lambda for test assertions
     */
    private fun testHeartbeat(
        agentStatusDtos: List<AgentStatusDto>,
        heartbeat: Heartbeat,
        tests: List<TestBatchDto>,
        mockAgentStatuses: Boolean = false,
        verification: () -> Unit,
    ) {
        // /updateAgentStatuses
        mockServer.enqueue(
            MockResponse().setResponseCode(200)
        )
        // /getTestBatches
        mockServer.enqueue(
            MockResponse()
                .setBody(Json.encodeToString(tests))
                .addHeader("Content-Type", "application/json")
        )
        if (mockAgentStatuses) {
            // /getAgentsStatusesForSameExecution
            mockServer.enqueue(
                MockResponse()
                    .setBody(
                        objectMapper.writeValueAsString(agentStatusDtos)
                    )
                    .addHeader("Content-Type", "application/json")
            )
        }

        webClient.post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(heartbeat))
            .exchange()
            .expectStatus().isOk

        // wait for background tasks
        Thread.sleep(2_000)

        verification.invoke()
    }

    companion object {
        @JvmStatic
        lateinit var mockServer: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServer.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            // todo: should be initialized in @BeforeAll, but it gets called after @DynamicPropertySource
            mockServer = MockWebServer()
            mockServer.start()
            registry.add("orchestrator.backendUrl") { "http://localhost:${mockServer.port}" }
        }
    }
}
