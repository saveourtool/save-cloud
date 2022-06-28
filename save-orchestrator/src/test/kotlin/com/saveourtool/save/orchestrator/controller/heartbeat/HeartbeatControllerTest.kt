package com.saveourtool.save.orchestrator.controller.heartbeat

import com.saveourtool.save.agent.*
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.orchestrator.config.Beans
import com.saveourtool.save.orchestrator.config.LocalDateTimeConfig
import com.saveourtool.save.orchestrator.controller.HeartbeatController
import com.saveourtool.save.orchestrator.controller.agentsLatestHeartBeatsMap
import com.saveourtool.save.orchestrator.controller.crashedAgents
import com.saveourtool.save.orchestrator.docker.AgentRunner
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.HeartBeatInspector
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.testsuite.TestSuiteType
import com.saveourtool.save.testutils.*

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.exist
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldNot
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.*
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@WebFluxTest(controllers = [HeartbeatController::class])
@Import(
    Beans::class,
    AgentService::class,
    HeartBeatInspector::class,
    LocalDateTimeConfig::class
)
@MockBeans(MockBean(AgentRunner::class))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnableScheduling
class HeartbeatControllerTest {
    @Autowired lateinit var webClient: WebTestClient
    @Autowired private lateinit var agentService: AgentService
    @MockBean private lateinit var dockerService: DockerService
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun webClientSetUp() {
        webClient = webClient
            .mutate()
            .responseTimeout(Duration.ofSeconds(2))
            .build()
    }

    @AfterEach
    fun cleanup() {
        mockServer.checkQueues()
        mockServer.cleanup()
        crashedAgents.clear()
        agentsLatestHeartBeatsMap.clear()
    }

    @Test
    fun checkAcceptingHeartbeat() {
        val heartBeatBusy = Heartbeat("test", AgentState.BUSY, ExecutionProgress(0), Clock.System.now() + 30.seconds)

        webClient.post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(heartBeatBusy)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should respond with NewJobResponse when there are tests`() {
        val list = listOf(TestDto("qwe", "WarnPlugin", 0, "hash", listOf("tag")))
        // /getTestBatches
        mockServer.enqueue(
            "/getTestBatches\\?agentId=.*",
            MockResponse()
                .setBody(Json.encodeToString(TestBatch(list, mapOf(0L to ""))))
                .addHeader("Content-Type", "application/json")
        )

        val testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
            id = 0
        }

        // /testSuite/{id}
        mockServer.enqueue(
            "/testSuite/(\\d)+",
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(testSuite))
        )

        val monoResponse = agentService.getNewTestsIds("container-1").block() as NewJobResponse

        assertTrue(monoResponse.tests.isNotEmpty())
        assertEquals("qwe", monoResponse.tests.first().filePath)
    }

    @Test
    fun `should not shutdown any agents when not all of them are IDLE`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "test-2"),
            ),
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100), Clock.System.now() + 30.seconds)),
            testBatch = TestBatch(emptyList(), emptyMap()),
            testSuite = null,
            mockAgentStatuses = true,
        ) { heartbeatResponses ->
            verify(dockerService, times(0)).stopAgents(anyCollection())
            heartbeatResponses shouldNot exist { it is TerminateResponse }
        }
    }

    @Test
    fun `should not shutdown any agents when all agents are IDLE but there are more tests left`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
            ),
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100), Clock.System.now() + 30.seconds)),
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) { heartbeatResponses ->
            verify(dockerService, times(0)).stopAgents(anyCollection())
            heartbeatResponses shouldNot exist { it is TerminateResponse }
        }
    }

    @Test
    fun `should send Terminate signal to idle agents when there are no tests left`() {
        whenever(dockerService.isAgentStopped(anyString())).thenReturn(true)
        val agentStatusDtos = listOf(
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100), Clock.System.now() + 30.seconds)),
            heartBeatInterval = 0,
            testBatch = TestBatch(emptyList(), emptyMap()),
            testSuite = null,
            mockAgentStatuses = true,
            {
                // /updateExecutionByDto
                mockServer.enqueue(
                    "/updateExecutionByDto.*",
                    MockResponse().setResponseCode(200)
                )
            }
        ) { heartbeatResponses ->
            heartbeatResponses.shouldHaveSingleElement { it is TerminateResponse }
            verify(
                dockerService,
                times(0).description("Orchestrator shouldn't stop agents if they stop heartbeating after TerminateResponse has been sent")
            ).stopAgents(anyCollection())
        }
    }

    @Test
    fun `should not shutdown any agents when they are STARTING`() {
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-2"),
            ),
            heartbeats = listOf(Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0), Clock.System.now() + 30.seconds)),
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) { heartbeatResponses ->
            verify(dockerService, times(0)).stopAgents(anyCollection())
            heartbeatResponses shouldNot exist { it is TerminateResponse }
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `should shutdown agent, which don't sent heartbeat for some time`() {
        whenever(dockerService.stopAgents(listOf(eq("test-1")))).thenReturn(true)
        whenever(dockerService.stopAgents(listOf(eq("test-2")))).thenReturn(false)
        val currTime = Clock.System.now()
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "test-2"),
            ),
            heartbeats = listOf(
                Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0), currTime),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0), currTime.plus(1.seconds)),
                Heartbeat("test-2", AgentState.BUSY, ExecutionProgress(0), currTime.plus(2.seconds)),
                // 3 absent heartbeats from test-2
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0), currTime.plus(3.seconds)),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0), currTime.plus(4.seconds)),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0), currTime.plus(10.seconds)),
            ),
            heartBeatInterval = 1_000,
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) {
            crashedAgents.shouldContainExactly(
                setOf("test-2")
            )
        }
    }
    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `should shutdown agent, which doesn't shutdown gracefully for some time, despite sending heartbeats`() {
        whenever(dockerService.stopAgents(listOf(eq("test-1")))).thenReturn(true)
        whenever(dockerService.stopAgents(listOf(eq("test-2")))).thenReturn(false)
        val currTime = Clock.System.now()
        testHeartbeat(
            agentStatusDtos = listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-1"),
                AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "test-2"),
            ),
            heartbeats = listOf(
                Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0), currTime),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0), currTime.plus(1.seconds)),
                Heartbeat("test-2", AgentState.BUSY, ExecutionProgress(0), currTime.plus(2.seconds)),
                // 3 absent heartbeats from test-2
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0), currTime.plus(3.seconds)),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0), currTime.plus(4.seconds)),
                Heartbeat("test-1", AgentState.BUSY, ExecutionProgress(0), currTime.plus(10.seconds)),
            ),
            heartBeatInterval = 1_000,
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) {
            crashedAgents.shouldContainExactly(
                setOf("test-2")
            )
        }
    }

    @Test
    fun `should shutdown all agents, since all of them don't sent heartbeats for some time`() {
        val agentStatusDtos = listOf(
            AgentStatusDto(LocalDateTime.now(), AgentState.STARTING, "test-1"),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(
                // heartbeats were sent long time ago
                Heartbeat("test-1", AgentState.STARTING, ExecutionProgress(0), Clock.System.now() - 1.minutes),
                Heartbeat("test-2", AgentState.BUSY, ExecutionProgress(0), Clock.System.now() - 1.minutes),
            ),
            heartBeatInterval = 0,
            testBatch = TestBatch(
                listOf(
                    TestDto("/path/to/test-1", "WarnPlugin", 1, "hash1", listOf("tag")),
                    TestDto("/path/to/test-2", "WarnPlugin", 1, "hash2", listOf("tag")),
                    TestDto("/path/to/test-3", "WarnPlugin", 1, "hash3", listOf("tag")),
                ),
                mapOf(1L to "")
            ),
            testSuite = TestSuite(TestSuiteType.PROJECT, "", null, null, LocalDateTime.now(), ".", ".").apply {
                id = 0
            },
            mockAgentStatuses = false,
        ) {
            crashedAgents shouldContainExactlyInAnyOrder setOf("test-1", "test-2")
        }
    }

    @Test
    fun `should shutdown agents even if there are some already FINISHED`() {
        val agentStatusDtos = listOf(
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
            AgentStatusDto(LocalDateTime.parse("2021-01-01T00:00:00"), AgentState.FINISHED, "test-1"),
            AgentStatusDto(LocalDateTime.parse("2021-01-01T00:00:00"), AgentState.FINISHED, "test-2"),
        )
        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(Heartbeat("test-1", AgentState.IDLE, ExecutionProgress(100), Clock.System.now() + 30.seconds)),
            heartBeatInterval = 0,
            testBatch = TestBatch(emptyList(), emptyMap()),
            testSuite = null,
            mockAgentStatuses = true,
            {
                mockServer.enqueue(
                    "/updateExecutionByDto.*",
                    MockResponse().setResponseCode(200)
                )
            }
        ) { heartbeatResponses ->
            heartbeatResponses.shouldHaveSingleElement { it is TerminateResponse }
            verify(
                dockerService,
                times(0).description("Orchestrator shouldn't stop agents if they stop heartbeating after TerminateResponse has been sent")
            ).stopAgents(anyCollection())
        }
    }

    @Suppress("TOO_LONG_FUNCTION")
    @Test
    fun `should mark test executions as failed if agent returned only part of results`() {
        val agentStatusDtos = listOf(
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-1"),
            AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "test-2"),
        )

        // if some test execution still have state `READY_FOR_TESTING`, but Agent.state == `FINISHED`
        // that's mean, that part of results is lost
        val testExecutions: List<TestExecutionDto> = listOf(
            TestExecutionDto(
                "testPath63",
                "WarnPlugin",
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

        // agentService.checkSavedData
        mockServer.enqueue(
            "/testExecutions/agent/test-1/${TestResultStatus.READY_FOR_TESTING}",
            MockResponse()
                .setBody(
                    objectMapper.writeValueAsString(
                        testExecutions
                    )
                )
                .addHeader("Content-Type", "application/json")
        )

        // agentService.markTestExecutionsAsFailed
        mockServer.enqueue(
            "/testExecution/setStatusByAgentIds/.*",
            MockResponse()
                .setResponseCode(200)
        )

        testHeartbeat(
            agentStatusDtos = agentStatusDtos,
            heartbeats = listOf(
                Heartbeat("test-1", AgentState.FINISHED, ExecutionProgress(100), Clock.System.now() + 30.seconds)
            ),
            heartBeatInterval = 0,
            testBatch = null,
            testSuite = null,
            mockAgentStatuses = false,
        ) {
            // not interested in any checks for heartbeats
        }
        val assertions = CompletableFuture.supplyAsync {
            listOf(
                mockServer.takeRequest(60, TimeUnit.SECONDS),
                mockServer.takeRequest(60, TimeUnit.SECONDS),
            )
        }
        assertions.orTimeout(60, TimeUnit.SECONDS).join().forEach { Assertions.assertNotNull(it) }
    }

    /**
     * Test logic triggered by a heartbeat.
     *
     * @param agentStatusDtos agent statuses that are returned from backend (mocked response)
     * @param heartbeats a [Heartbeat] that is received by orchestrator
     * @param testBatch a batch of tests returned from backend (mocked response)
     * @param mockAgentStatuses whether a mocked response for `/getAgentsStatusesForSameExecution` should be added to queue
     * @param additionalSetup is executed before the request is performed
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
        testBatch: TestBatch?,
        testSuite: TestSuite?,
        mockAgentStatuses: Boolean = false,
        additionalSetup: () -> Unit = {},
        verification: (heartbeatResponses: List<HeartbeatResponse?>) -> Unit,
    ) {
        // /getTestBatches
        testBatch?.let {
            mockServer.enqueue(
                "/getTestBatches\\?agentId=.*",
                MockResponse()
                    .setBody(Json.encodeToString(testBatch))
                    .addHeader("Content-Type", "application/json")
            )
        }

        // /testSuite/{id}
        testSuite?.let {
            mockServer.enqueue(
                "/testSuite/(\\d)+",
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(objectMapper.writeValueAsString(testSuite))
            )
        }

        /* mockServer.enqueue(
            "/agents/[^/]+/execution/id",
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(99))
        )*/

        if (mockAgentStatuses) {
            // /getAgentsStatusesForSameExecution
            mockServer.enqueue(
                "/getAgentsStatusesForSameExecution.*",
                MockResponse()
                    .setBody(
                        objectMapper.writeValueAsString(
                            AgentStatusesForExecution(0, agentStatusDtos)
                        )
                    )
                    .addHeader("Content-Type", "application/json")
            )
            mockServer.setDefaultResponseForPath(
                "/agents/statuses.*",
                MockResponse()
                    .setBody(
                        objectMapper.writeValueAsString(
                            agentStatusDtos
                        )
                    )
                    .addHeader("Content-Type", "application/json")
            )
        }
        additionalSetup()
        val assertions = CompletableFuture.supplyAsync {
            buildList<RecordedRequest?> {
                mockServer.takeRequest(60, TimeUnit.SECONDS)
                testBatch?.let {
                    mockServer.takeRequest(60, TimeUnit.SECONDS)
                }
                if (mockAgentStatuses) {
                    mockServer.takeRequest(60, TimeUnit.SECONDS)
                }
            }
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

        assertions.orTimeout(60, TimeUnit.SECONDS).join().forEach { Assertions.assertNotNull(it) }
        verification.invoke(heartbeatResponses)
    }

    companion object {
        @JvmStatic
        private lateinit var mockServer: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServer.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            // todo: should be initialized in @BeforeAll, but it gets called after @DynamicPropertySource
            mockServer = createMockWebServer()
            mockServer.setDefaultResponseForPath("/testExecution/.*", MockResponse().setResponseCode(200))
            mockServer.setDefaultResponseForPath("/updateAgentStatusesWithDto", MockResponse().setResponseCode(200))
            mockServer.start()
            registry.add("orchestrator.backendUrl") { "http://localhost:${mockServer.port}" }
        }
    }
}
