package org.cqfn.save.orchestrator.controller.heartbeat

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.ExecutionProgress
import org.cqfn.save.agent.Heartbeat
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.entities.Agent
import org.cqfn.save.entities.AgentStatus
import org.cqfn.save.orchestrator.config.Beans
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService
import org.cqfn.save.test.TestDto

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
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
        val list = listOf(TestDto("qwe", 0, 1))
        mockServer.enqueue(
            MockResponse()
                .setBody(Json.encodeToString(list))
                .addHeader("Content-Type", "application/json")
        )

        val monoResponse = agentService.setNewTestsIds().block() as NewJobResponse

        assertTrue(monoResponse.ids.isNotEmpty() && monoResponse.ids.first().filePath == "qwe")
    }

    @Test
    fun `should shutdown idle agents when there are no tests left`() {
        val heartbeatFinished = Heartbeat("test", AgentState.IDLE, ExecutionProgress(100))
        // /updateAgentStatuses
        mockServer.enqueue(
            MockResponse().setResponseCode(200)
        )
        // /getTestBatches
        mockServer.enqueue(
            MockResponse()
                .setBody(Json.encodeToString(emptyList<TestDto>()))
                .addHeader("Content-Type", "application/json")
        )
        // /getAgentsStatusesForSameExecution
        mockServer.enqueue(
            MockResponse()
                .setBody(
                    objectMapper.writeValueAsString(
                        listOf(AgentStatus(LocalDateTime.now(), AgentState.IDLE, Agent("test", null)))
                    )
                )
                .addHeader("Content-Type", "application/json")
        )

        webClient.post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(heartbeatFinished))
            .exchange()
            .expectStatus().isOk

        // wait for background tasks
        Thread.sleep(2_000)

        verify(dockerService).stopAgents(any())
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
