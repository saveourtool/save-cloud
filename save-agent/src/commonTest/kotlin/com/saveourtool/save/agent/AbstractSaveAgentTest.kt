package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.fs
import com.saveourtool.save.agent.utils.updateFromEnv
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.logging.logType
import com.saveourtool.save.reporter.Report
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")
open class AbstractSaveAgentTest {
    @Suppress("MAGIC_NUMBER", "MagicNumber")
    private val tmpDir: okio.Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
        .resolve("save-agent-test-${Random.nextInt(100, 999)}")
    private val reportFile = tmpDir / "save.out.json"

    private val configuration: AgentConfiguration by lazy {
        AgentConfiguration.initializeFromEnv()
            .updateFromEnv()
            .let {
                it.copy(
                    save = it.save.copy(
                        reportDir = tmpDir.toString()
                    )
                )
            }
    }

    private val saveAgentForTest: SaveAgent by lazy {
        SaveAgent(configuration, httpClient = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/heartbeat" -> respond(
                            json.encodeToString(PolymorphicSerializer(HeartbeatResponse::class), ContinueResponse),
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        )
                        else -> error("Unhandled ${request.url}")
                    }
                }
            }
        }, coroutineScope = CoroutineScope(Dispatchers.Default))
    }

    @BeforeTest
    fun setUp() {
        logType.set(LogType.ALL)
        val report = Report(
            testSuite = "Doing nothing it test mode",
            pluginExecutions = emptyList(),
        )
        fs.createDirectory(tmpDir)
        fs.write(reportFile, true) {
            writeUtf8(Json.encodeToString(listOf(report)))
        }
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    @Test
    fun `agent should send heartbeats`() {
        runBlocking {
            saveAgentForTest.sendHeartbeat(ExecutionProgress(0, -1L))
        }
    }

    @Test
    fun `should change state to FINISHED after SAVE CLI returns`() = runBlocking {
        assertEquals(AgentState.BUSY, saveAgentForTest.state.get())
        runBlocking {
            saveAgentForTest.run {
                startSaveProcess(AgentRunConfig("", "N/A", "N/A"))
            }
        }
        assertEquals(AgentState.FINISHED, saveAgentForTest.state.get())
    }
}
