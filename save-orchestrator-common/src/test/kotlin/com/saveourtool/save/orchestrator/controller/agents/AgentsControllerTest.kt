package com.saveourtool.save.orchestrator.controller.agents

import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.TestingType
import com.saveourtool.save.orchestrator.SAVE_AGENT_VERSION
import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.service.OrchestratorAgentService
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.ContainerService

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

import org.springframework.http.ResponseEntity
import reactor.kotlin.core.publisher.toMono

@WebFluxTest(controllers = [AgentsController::class])
@Import(AgentService::class)
@MockBeans(MockBean(ContainerRunner::class))
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentsControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean private lateinit var containerService: ContainerService
    @MockBean private lateinit var orchestratorAgentService: OrchestratorAgentService
    @MockBean private lateinit var containerRunner: ContainerRunner

    @Test
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "UnsafeCallOnNullableType")
    fun `should build image, query backend and start containers`() {
        val project = Project.stub(null)
        val execution = Execution.stub(project).apply {
            type = TestingType.PUBLIC_TESTS
            status = ExecutionStatus.PENDING
            id = 42L
        }
        whenever(containerService.prepareConfiguration(any())).thenReturn(
            ContainerService.RunConfiguration(
                imageTag = "test-image-id",
                runCmd = listOf("sh", "-c", "test-exec-cmd"),
                workingDir = EXECUTION_DIR,
                env = emptyMap(),
            )
        )
        whenever(containerService.createContainers(any(), any()))
            .thenReturn(listOf("test-agent-id-1", "test-agent-id-2"))

        whenever(containerRunner.getContainerIdentifier(any())).thenReturn("save-test-agent-id-1")

        whenever(containerService.startContainersAndUpdateExecution(any(), anyList()))
            .thenReturn(true.toMono())
        whenever(orchestratorAgentService.addAgent(anyLong(), any()))
            .thenReturn(ResponseEntity.ok().build<Void>().toMono())
        whenever(orchestratorAgentService.updateAgentStatus(any()))
            .thenReturn(ResponseEntity.ok().build<Void>().toMono())
        // /updateExecutionByDto is not mocked, because it's performed by DockerService, and it's mocked in these tests

        webClient
            .post()
            .uri("/initializeAgents")
            .bodyValue(execution.toRunRequest(SAVE_AGENT_VERSION, "someUrl"))
            .exchange()
            .expectStatus()
            .isAccepted
        Thread.sleep(2_500)  // wait for background task to complete on mocks
        verify(containerService).prepareConfiguration(any())
        verify(containerService).createContainers(any(), any())
        verify(containerService).startContainersAndUpdateExecution(any(), anyList())
    }

    @Test
    fun checkPostResponseIsNotOk() {
        val project = Project.stub(null)
        val execution = Execution.stub(project)

        assertThrows<IllegalArgumentException> {
            execution.toRunRequest(SAVE_AGENT_VERSION, "someUrl")
        }
    }

    @Test
    fun `should cleanup execution artifacts`() {
        webClient.post()
            .uri("/cleanup?executionId=42")
            .exchange()
            .expectStatus()
            .isOk

        Thread.sleep(2_500)
        verify(containerService, times(1)).cleanup(anyLong())
    }
}
