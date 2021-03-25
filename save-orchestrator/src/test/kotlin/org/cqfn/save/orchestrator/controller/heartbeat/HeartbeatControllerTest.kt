package org.cqfn.save.orchestrator.controller.heartbeat

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.ExecutionProgress
import org.cqfn.save.agent.Heartbeat
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.test.TestDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.Duration

@WebFluxTest
@Import(AgentService::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HeartbeatControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient
    lateinit var mockServer: MockWebServer
    private lateinit var agentService: AgentService
    private val objectMapper = ObjectMapper()

    @BeforeAll
    fun startServer() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    @AfterAll
    fun stopServer() {
        mockServer.shutdown()
    }

    @BeforeEach
    fun webClientSetUp() {
        webClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
    }

    @BeforeEach
    fun initialize() {
        val baseUrl = "http://localhost:${mockServer.port}"
        agentService = AgentService(baseUrl)
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
        val list = listOf(TestDto("qwe", "www", 0, "hashID"))

        mockServer.enqueue(
                MockResponse()
                    .setBody(objectMapper.writeValueAsString(list))
                    .addHeader("Content-Type", "application/json")
        )

        val monoResponse = agentService.setNewTestsIds()

        assertTrue((monoResponse.block() as NewJobResponse).ids.isNotEmpty())
    }
}
