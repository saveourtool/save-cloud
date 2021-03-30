package org.cqfn.save.orchestrator.controller.agents

import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.service.AgentService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime

@WebFluxTest
@Import(AgentService::class)
class InitializeAgentsTest {
    private val stubTime = LocalDateTime.now()

    @Autowired
    lateinit var webClient: WebTestClient

    @Test
    fun checkPostResponseIsOk() {
        val execution = Execution(3, stubTime, stubTime, ExecutionStatus.PENDING, "stub", "stub")
        webClient
            .post()
            .uri("/initializeAgents")
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk
    }
}
