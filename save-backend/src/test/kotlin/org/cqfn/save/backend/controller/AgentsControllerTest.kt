package org.cqfn.save.backend.controller

import org.cqfn.save.agent.AgentState
import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.AgentStatusDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime
import javax.transaction.Transactional

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class AgentsControllerTest {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var agentStatusRepository: AgentStatusRepository

    @Test
    fun `should save agent statuses`() {
        webTestClient
            .method(HttpMethod.POST)
            .uri("/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "container-1")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @Transactional
    @Suppress("TOO_LONG_FUNCTION")
    fun `check that agent statuses are updated`() {
        webTestClient
            .method(HttpMethod.POST)
            .uri("/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "container-2")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk

        webTestClient
            .method(HttpMethod.POST)
            .uri("/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "container-2")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk

        webTestClient
            .method(HttpMethod.POST)
            .uri("/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "container-2")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk

        assertTrue(
            agentStatusRepository
                .findAll()
                .filter { it.state == AgentState.IDLE && it.agent.containerId == "container-2" }
                .size == 1
        )
    }

    @Test
    fun `should return latest status by container id`() {
        webTestClient
            .method(HttpMethod.GET)
            .uri("/getAgentsStatusesForSameExecution")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue("container-1")
            .exchange()
            .expectBody<List<AgentStatusDto?>>()
            .consumeWith {
                val statuses = it.responseBody
                requireNotNull(statuses)
                Assertions.assertEquals(2, statuses.size)
                Assertions.assertEquals(AgentState.IDLE, statuses.first()!!.state)
                Assertions.assertEquals(AgentState.BUSY, statuses[1]!!.state)
            }
    }
}
