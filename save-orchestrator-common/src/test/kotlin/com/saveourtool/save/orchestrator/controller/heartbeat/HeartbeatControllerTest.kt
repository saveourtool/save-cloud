package com.saveourtool.save.orchestrator.controller.heartbeat

import com.saveourtool.save.agent.*
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.*
import com.saveourtool.save.orchestrator.config.JsonConfig
import com.saveourtool.save.orchestrator.controller.HeartbeatController
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.ContainerService
import com.saveourtool.save.orchestrator.service.HeartBeatInspector
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto

import com.saveourtool.save.orchestrator.service.OrchestratorAgentService
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldNot
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.time.Duration

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import org.mockito.kotlin.*
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Month

@Suppress("ReactiveStreamsUnusedPublisher")
@WebFluxTest(controllers = [HeartbeatController::class])
@Import(
    AgentService::class,
    HeartBeatInspector::class,
    JsonConfig::class,
)
@MockBeans(MockBean(ContainerRunner::class))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnableScheduling
class HeartbeatControllerTest {
    @Autowired lateinit var webClient: WebTestClient
    @Autowired private lateinit var agentService: AgentService
    @MockBean private lateinit var containerService: ContainerService
    @Autowired private lateinit var heartBeatInspector: HeartBeatInspector
    @MockBean private lateinit var orchestratorAgentService: OrchestratorAgentService

    @BeforeEach
    fun webClientSetUp() {
        webClient = webClient
            .mutate()
            .responseTimeout(Duration.ofSeconds(2))
            .build()
    }

    @AfterEach
    fun cleanup() {
        verifyNoMoreInteractions(orchestratorAgentService)
        heartBeatInspector.clear()
    }

    @Test
    fun checkAcceptingHeartbeat() {
        val heartBeatBusy = Heartbeat("test", AgentState.BUSY, ExecutionProgress(0, -1L), Clock.System.now() + 30.seconds)

        whenever(orchestratorAgentService.updateAgentStatusesWithDto(any()))
            .thenReturn(ResponseEntity.ok().build<Void>().toMono())
        webClient.post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(heartBeatBusy)
            .exchange()
            .expectStatus()
            .isOk
        verify(orchestratorAgentService).updateAgentStatusesWithDto(any())
    }

    @Test
    fun `should respond with NewJobResponse when there are tests`() {
        val agentContainerId = "container-1"
        val cliArgs = "qwe"
        whenever(orchestratorAgentService.getNextRunConfig(agentContainerId))
            .thenReturn(
                AgentRunConfig(
                    cliArgs = cliArgs,
                    executionDataUploadUrl = "N/A",
                    debugInfoUploadUrl = "N/A"
                ).toMono()
            )

        val monoResponse = agentService.getNextRunConfig(agentContainerId).block() as NewJobResponse

        assertTrue(monoResponse.config.cliArgs.isNotEmpty())
        assertEquals(cliArgs, monoResponse.config.cliArgs)
        verify(orchestratorAgentService).getNextRunConfig(any())
    }

    @Test
    fun `should not shutdown any agents when not all of them are IDLE`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(AgentState.IDLE, "test-1"),
                AgentStatusDto(AgentState.BUSY, "test-2"),
            ),
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100, -1L), Clock.System.now() + 30.seconds)),
            initConfigs = emptyList(),
            testBatchNullable = emptyList(),
            mockUpdateAgentStatusesCount = 1,
            mockAgentStatusesForSameExecution = true,
        ) { heartbeatResponses ->
            verify(containerService, times(0)).stopAgents(any())
            heartbeatResponses shouldNot exist { it is TerminateResponse }
        }
    }

    @Test
    fun `should not shutdown any agents when all agents are IDLE but there are more tests left`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(AgentState.IDLE, "test-1"),
                AgentStatusDto(AgentState.IDLE, "test-2"),
            ),
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100, -1L), Clock.System.now() + 30.seconds)),
            initConfigs = emptyList(),
            testBatchNullable = listOf(
                TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
            ),
            mockUpdateAgentStatusesCount = 2,
        ) { heartbeatResponses ->
            verify(containerService, times(0)).stopAgents(any())
            heartbeatResponses shouldNot exist { it is TerminateResponse }
        }
    }

    @Test
    fun `should send Terminate signal to idle agents when there are no tests left`() {
        whenever(containerService.isStoppedByContainerId(any())).thenReturn(true)
        val agentStatusDtos = listOf(
            AgentStatusDto(AgentState.IDLE, "test-1"),
            AgentStatusDto(AgentState.IDLE, "test-2"),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100, -1L), Clock.System.now() + 30.seconds)),
            heartBeatInterval = 0,
            initConfigs = emptyList(),
            testBatchNullable = emptyList(),
            mockUpdateAgentStatusesCount = 2,
            mockAgentStatusesForSameExecution = true,
        ) { heartbeatResponses ->
            heartbeatResponses.shouldHaveSingleElement { it is TerminateResponse }
            verify(
                containerService,
                times(0).description("sandbox shouldn't stop agents if they stop heartbeating after TerminateResponse has been sent")
            ).stopAgents(any())
        }
    }

    @Test
    fun `should not shutdown any agents when they are STARTING`() {
        val currTime = Clock.System.now()
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(AgentState.STARTING, "test-1"),
                AgentStatusDto(AgentState.STARTING, "test-2"),
            ),
            heartbeats = listOf(
                Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0, -1L), currTime + 1.seconds),
                Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(0, -1L), currTime + 2.seconds)
            ),
            initConfigs = listOf(initConfig),
            testBatchNullable = listOf(
                TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
            ),
            mockUpdateAgentStatusesCount = 3,
        ) { heartbeatResponses ->
            verify(containerService, times(0)).stopAgents(any())
            heartbeatResponses shouldNot exist { it is TerminateResponse }
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `should shutdown agent, which don't sent heartbeat for some time`() {
        whenever(containerService.stopAgents(listOf(eq("test-1")))).thenReturn(true)
        whenever(containerService.stopAgents(listOf(eq("test-2")))).thenReturn(false)
        val currTime = Clock.System.now()
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(AgentState.STARTING, "test-1"),
                AgentStatusDto(AgentState.BUSY, "test-2"),
            ),
            heartbeats = listOf(
                Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0, -1L), currTime),
                Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(0, -1L), currTime + 1.seconds),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0, -1L), currTime + 2.seconds),
                Heartbeat("test-2", AgentState.BUSY, ExecutionProgress(0, -1L), currTime + 3.seconds),
                // 3 absent heartbeats from test-2
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0, -1L), currTime + 4.seconds),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0, -1L), currTime + 5.seconds),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0, -1L), currTime + 10.seconds),
            ),
            heartBeatInterval = 1_000,
            initConfigs = listOf(initConfig),
            testBatchNullable = listOf(
                TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
            ),
            mockUpdateAgentStatusesCount = 8,
        ) {
            heartBeatInspector.crashedAgents.shouldContainExactly(
                setOf("test-2")
            )
        }
    }

    @Test
    fun `should shutdown all agents, since all of them don't sent heartbeats for some time`() {
        val agentStatusDtos = listOf(
            AgentStatusDto(AgentState.STARTING, "test-1"),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(
                // heartbeats were sent long time ago
                Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0, -1L), Clock.System.now() - 1.minutes),
                Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(0, -1L), Clock.System.now() - 1.minutes),
                Heartbeat("test-2", AgentState.BUSY, ExecutionProgress(0, -1L), Clock.System.now() - 1.minutes),
            ),
            heartBeatInterval = 0,
            initConfigs = listOf(initConfig),
            testBatchNullable = listOf(
                TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
            ),
            mockUpdateAgentStatusesCount = 4,
        ) {
            heartBeatInspector.crashedAgents shouldContainExactlyInAnyOrder setOf("test-1", "test-2")
        }
    }

    @Test
    fun `should shutdown agents even if there are some already FINISHED`() {
        val agentStatusDtos = listOf(
            AgentStatusDto(AgentState.IDLE, "test-1"),
            AgentStatusDto(AgentState.IDLE, "test-2"),
            AgentStatusDto(AgentState.FINISHED, "test-1", LocalDateTime(2021, Month.JANUARY, 1, 0, 0, 0)),
            AgentStatusDto(AgentState.FINISHED, "test-2", LocalDateTime(2021, Month.JANUARY, 1, 0, 0, 0)),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100, -1L), Clock.System.now() + 30.seconds)),
            heartBeatInterval = 0,
            initConfigs = emptyList(),
            testBatchNullable = emptyList(),
            mockUpdateAgentStatusesCount = 2,
            mockAgentStatusesForSameExecution = true,
        ) { heartbeatResponses ->
            heartbeatResponses.shouldHaveSingleElement { it is TerminateResponse }
            verify(
                containerService,
                times(0).description("sandbox shouldn't stop agents if they stop heartbeating after TerminateResponse has been sent")
            ).stopAgents(any())
        }
    }

    @Suppress("TOO_LONG_FUNCTION")
    @Test
    fun `should mark test executions as failed if agent returned only part of results`() {
        val agentStatusDtos = listOf(
            AgentStatusDto(AgentState.IDLE, "test-1"),
            AgentStatusDto(AgentState.IDLE, "test-2"),
        )

        // if some test execution still have state `READY_FOR_TESTING`, but Agent.state == `FINISHED`
        // that's mean, that part of results is lost
        val testExecutions: List<TestExecutionDto> = listOf(
            TestExecutionDto(
                "testPath63",
                "WarnPlugin",
                "test",
                "test",
                TestResultStatus.READY_FOR_TESTING,
                0,
                0,
                unmatched = 3,
                matched = 2,
                expected = 0,
                unexpected = 0,
            )
        )

        doReturn(Mono.just(testExecutions))
            .whenever(orchestratorAgentService)
            .getReadyForTestingTestExecutions(argThat { this == "test-1" })

        whenever(orchestratorAgentService.markTestExecutionsOfAgentsAsFailed(any(), any()))
            .thenReturn(Mono.just(ResponseEntity.ok().build()))

        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(
                Heartbeat("test-1", AgentState.FINISHED, ExecutionProgress(100, -1L), Clock.System.now() + 30.seconds)
            ),
            heartBeatInterval = 0,
            initConfigs = emptyList(),
            testBatchNullable = null,
            mockUpdateAgentStatusesCount = 1
        ) {
            // not interested in any checks for heartbeats
            verify(orchestratorAgentService).getReadyForTestingTestExecutions(any())
            verify(orchestratorAgentService).markTestExecutionsOfAgentsAsFailed(any(), any())
        }
    }

    /**
     * Test logic triggered by a heartbeat.
     *
     * @param agentStatusDtos agent statuses that are returned from backend (mocked response)
     * @param heartbeats a [Heartbeat] that is received by sandbox
     * @param testBatchNullable a batch of tests returned from backend (mocked response)
     * @param mockAgentStatusesForSameExecution whether a mocked response for `/getAgentsStatusesForSameExecution` should be added to queue
     * @param verification a lambda for test assertions
     */
    @Suppress(
        "TOO_LONG_FUNCTION",
        "TOO_MANY_PARAMETERS",
        "LongParameterList",
        "LongMethod"
    )
    private fun testHeartbeat(
        agentStatusDtos: List<AgentStatusDto>,
        heartbeats: List<Heartbeat>,
        heartBeatInterval: Long = 0,
        initConfigs: List<AgentInitConfig>,
        testBatchNullable: TestBatch?,
        mockUpdateAgentStatusesCount: Int = 0,
        mockAgentStatusesForSameExecution: Boolean = false,
        verification: (heartbeatResponses: List<HeartbeatResponse?>) -> Unit,
    ) {
        initConfigs.forEach {
            whenever(orchestratorAgentService.getInitConfig(any()))
                .thenReturn(Mono.just(it))
        }
        testBatchNullable?.let { testBatch ->
            val returnValue = if (testBatch.isNotEmpty()) {
                AgentRunConfig(
                    cliArgs = testBatch.joinToString(" ") { it.filePath },
                    executionDataUploadUrl = "N/A",
                    debugInfoUploadUrl = "N/A",
                ).toMono()
            } else {
                Mono.empty()
            }
            whenever(orchestratorAgentService.getNextRunConfig(any()))
                .thenReturn(returnValue)
        }

        repeat(mockUpdateAgentStatusesCount) {
            whenever(orchestratorAgentService.updateAgentStatusesWithDto(any()))
                .thenReturn(ResponseEntity.ok().build<Void>().toMono())
        }
        if (mockAgentStatusesForSameExecution) {
            whenever(orchestratorAgentService.getAgentsStatusesForSameExecution(any()))
                .thenReturn(Mono.just(AgentStatusesForExecution(0, agentStatusDtos)))
        }

        val heartbeatResponses: MutableList<HeartbeatResponse?> = mutableListOf()
        heartbeats.forEach { heartbeat ->
            webClient.post()
                .uri("/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(heartbeat))
                .exchange()
                .expectAll({ responseSpec ->
                    responseSpec.expectBody<HeartbeatResponse>()
                        .consumeWith {
                            heartbeatResponses.add(it.responseBody)
                        }
                },
                    { responseSpec ->
                        responseSpec.expectStatus()
                            .isOk
                    }
                )
            Thread.sleep(heartBeatInterval)
        }

        // wait for background tasks
        Thread.sleep(5_000)

        verify(orchestratorAgentService, times(initConfigs.size)).getInitConfig(any())
        heartbeatResponses.filterIsInstance<InitResponse>().shouldHaveSize(initConfigs.size)
        testBatchNullable?.let {
            verify(orchestratorAgentService).getNextRunConfig(any())
        }
        verify(orchestratorAgentService, times(mockUpdateAgentStatusesCount)).updateAgentStatusesWithDto(any())
        if (mockAgentStatusesForSameExecution) {
            verify(orchestratorAgentService).getAgentsStatusesForSameExecution(any())
        }
        verification.invoke(heartbeatResponses)
    }

    companion object {
        private val initConfig: AgentInitConfig = AgentInitConfig(
            saveCliUrl = "stub",
            testSuitesSourceSnapshotUrl = "stub",
            additionalFileNameToUrl = mapOf("file" to "stub"),
            saveCliOverrides = SaveCliOverrides(),
        )
    }
}
