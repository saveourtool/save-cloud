package org.cqfn.save.orchestrator.controller.agents

import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.config.Beans
import org.cqfn.save.orchestrator.controller.AgentsController
import org.cqfn.save.orchestrator.service.AgentService
import org.cqfn.save.orchestrator.service.DockerService

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

import java.time.LocalDateTime

@WebFluxTest(controllers = [AgentsController::class])
@Import(AgentService::class, Beans::class)
class AgentsControllerTest {
    private val stubTime = LocalDateTime.now()

    @Autowired
    lateinit var webClient: WebTestClient
    @MockBean private lateinit var dockerService: DockerService

    @Test
    fun checkPostResponseIsOk() {
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val execution = Execution(project, stubTime, stubTime, ExecutionStatus.PENDING, "stub", "stub", 0, 20).apply {
            id = 42L
        }
        whenever(dockerService.buildAndCreateContainers(any())).thenReturn(listOf("test-agent-id-1", "test-agent-id-2"))
        webClient
            .post()
            .uri("/initializeAgents")
            .body(BodyInserters.fromValue(execution))
            .exchange()
            .expectStatus()
            .isOk
        Thread.sleep(1_000)  // wait for background task to complete on mocks
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
}
