package com.saveourtool.save.agent

import generated.SAVE_CORE_VERSION
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.*
import platform.posix.setenv

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.PolymorphicSerializer

@Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")
class SaveAgentTest {
    init {
        setenv(AgentEnvName.AGENT_ID.name, "agent-for-test", 1)
        setenv(AgentEnvName.BACKEND_URL.name, "http://localhost:5800", 1)
        setenv(AgentEnvName.ORCHESTRATOR_URL.name, "http://localhost:5100", 1)
        setenv(AgentEnvName.CLI_COMMAND.name, "echo Doing nothing it test mode", 1)
    }

    private val configuration: AgentConfiguration = AgentConfiguration.initializeFromEnv().let {
        if (Platform.osFamily == OsFamily.WINDOWS) it.copy(cliCommand = "save-$SAVE_CORE_VERSION-linuxX64.bat") else it
    }
    private val saveAgentForTest = SaveAgent(configuration, httpClient = HttpClient(MockEngine) {
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
                    "/executionData" -> respond("", status = HttpStatusCode.OK)
                    "/executionLogs" -> respond("", status = HttpStatusCode.OK)
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    }, coroutineScope = CoroutineScope(Dispatchers.Default))

    @BeforeTest
    fun setUp() {
        if (Platform.osFamily != OsFamily.WINDOWS) {
            platform.posix.system("echo echo 0 > save-$SAVE_CORE_VERSION-linuxX64.kexe")
            platform.posix.system("chmod +x save-$SAVE_CORE_VERSION-linuxX64.kexe")
        } else {
            platform.posix.system("echo echo 0 > save-$SAVE_CORE_VERSION-linuxX64.bat")
        }
    }

    @AfterTest
    fun tearDown() {
        platform.posix.system("rm -rf save-$SAVE_CORE_VERSION-linuxX64.kexe")
    }

    @Test
    fun `agent should send heartbeats`() {
        runBlocking {
            saveAgentForTest.sendHeartbeat(ExecutionProgress(0, -1L))
        }
    }

    @Test
    fun `should change state to FINISHED after SAVE CLI returns`() = runBlocking {
        assertEquals(AgentState.BUSY, saveAgentForTest.state.value)
        runBlocking { saveAgentForTest.run { startSaveProcess("") } }
        assertEquals(AgentState.FINISHED, saveAgentForTest.state.value)
    }
}
