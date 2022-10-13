package com.saveourtool.save.orchestrator.controller.agents

import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.TestingType
import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.orchestrator.runner.AgentRunner
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.service.AgentRepository
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux

import org.springframework.http.ResponseEntity
import reactor.kotlin.core.publisher.toMono

@WebFluxTest(controllers = [AgentsController::class])
@Import(AgentService::class)
@MockBeans(MockBean(AgentRunner::class))
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentsControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean private lateinit var dockerService: DockerService
    @MockBean private lateinit var agentRepository: AgentRepository
    @MockBean private lateinit var agentRunner: AgentRunner

    @Test
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "UnsafeCallOnNullableType")
    fun `should build image, query backend and start containers`() {
        val project = Project.stub(null)
        val execution = Execution.stub(project).apply {
            type = TestingType.PUBLIC_TESTS
            status = ExecutionStatus.PENDING
            id = 42L
        }
        whenever(dockerService.prepareConfiguration(any())).thenReturn(
            DockerService.RunConfiguration(
                imageTag = "test-image-id",
                runCmd = listOf("sh", "-c", "test-exec-cmd"),
                workingDir = EXECUTION_DIR,
                env = emptyMap(),
            )
        )
        whenever(dockerService.createContainers(any(), any()))
            .thenReturn(listOf("test-agent-id-1", "test-agent-id-2"))

        whenever(agentRunner.getContainerIdentifier(any())).thenReturn("save-test-agent-id-1")

        whenever(dockerService.startContainersAndUpdateExecution(any(), anyList()))
            .thenReturn(Flux.just(1L, 2L, 3L))
        whenever(agentRepository.addAgents(anyList()))
            .thenReturn(listOf<Long>(1, 2).toMono())
        whenever(agentRepository.updateAgentStatusesWithDto(anyList()))
            .thenReturn(ResponseEntity.ok().build<Void>().toMono())
        // /updateExecutionByDto is not mocked, because it's performed by DockerService, and it's mocked in these tests

        webClient
            .post()
            .uri("/initializeAgents")
            .bodyValue(execution.toRunRequest())
            .exchange()
            .expectStatus()
            .isAccepted
        Thread.sleep(2_500)  // wait for background task to complete on mocks
        verify(dockerService).prepareConfiguration(any())
        verify(dockerService).createContainers(any(), any())
        verify(dockerService).startContainersAndUpdateExecution(any(), anyList())
    }

    @Test
    fun checkPostResponseIsNotOk() {
        val project = Project.stub(null)
        val execution = Execution.stub(project)

        assertThrows<IllegalArgumentException> {
            execution.toRunRequest()
        }
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
    fun `should cleanup execution artifacts`() {
        webClient.post()
            .uri("/cleanup?executionId=42")
            .exchange()
            .expectStatus()
            .isOk

        Thread.sleep(2_500)
        verify(dockerService, times(1)).cleanup(anyLong())
    }
}
