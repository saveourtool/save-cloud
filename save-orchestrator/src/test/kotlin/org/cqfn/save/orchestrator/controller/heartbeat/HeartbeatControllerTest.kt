package org.cqfn.save.orchestrator.controller.heartbeat

import org.cqfn.save.orchestrator.model.AgentState
import org.cqfn.save.orchestrator.model.Heartbeat
import org.cqfn.save.orchestrator.model.NewJobResponse
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
        val heartBeatBusy = Heartbeat("test", AgentState.BUSY, 0)

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
        val heartBeatIdle = Heartbeat("idle", AgentState.IDLE, 0)

        webClient.post()
                .uri("/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(heartBeatIdle), Heartbeat::class.java)
                .exchange()
                .expectBody(NewJobResponse::class.java)
                .isEqualTo<Nothing>(NewJobResponse(emptyList()))
    }
}