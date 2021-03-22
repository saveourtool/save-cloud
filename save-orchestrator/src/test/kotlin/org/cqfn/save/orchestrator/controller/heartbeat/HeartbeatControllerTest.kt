package org.cqfn.save.orchestrator.controller.heartbeat

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.ExecutionProgress
import org.cqfn.save.agent.Heartbeat
import org.cqfn.save.orchestrator.service.AgentService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest
@Import(AgentService::class)
class HeartbeatControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

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
        val heartBeatIdle = Heartbeat("idle", AgentState.IDLE, ExecutionProgress(0))

        webClient.post()
                .uri("/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(heartBeatIdle), Heartbeat::class.java)
                .exchange()
                .expectBody(TestNewJobResponse::class.java)
                .isEqualTo<Nothing>(TestNewJobResponse("org.cqfn.save.agent.NewJobResponse"))
    }
}

data class TestNewJobResponse(val type: String? = null) {
    val ids: List<String> = emptyList()
}