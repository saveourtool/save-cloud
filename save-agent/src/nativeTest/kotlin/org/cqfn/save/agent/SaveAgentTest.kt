package org.cqfn.save.agent

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveAgentTest {
    private val saveAgentForTest = SaveAgent(httpClient = HttpClient(MockEngine) {
        install(JsonFeature)
        engine {
            addHandler { request ->
                when(request.url.encodedPath) {
                    "/heartbeat" -> respond("", status = HttpStatusCode.OK)
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    })

    @Test
    fun `agent should send heartbeats`() {
        runBlocking {
            saveAgentForTest.sendHeartbeat()
        }
    }

    @Test
    fun `should change state to FINISHED after SAVE CLI returns`() = runBlocking {
        assertEquals(AgentState.IDLE, saveAgentForTest.state.value)
        launch { saveAgentForTest.start() }
        println("Waiting for 5 sec in test")
        delay(5_000)  // todo: proper criterion of SAVE CLI termination
        assertEquals(AgentState.FINISHED, saveAgentForTest.state.value)
        saveAgentForTest.stop()
    }
}
