@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import org.cqfn.save.agent.utils.readProperties

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import platform.posix.system
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@Suppress("INLINE_CLASS_CAN_BE_USED")
class SaveAgentTest {
    @OptIn(ExperimentalSerializationApi::class)
    private val configuration: AgentConfiguration = Properties.decodeFromStringMap(readProperties("src/nativeMain/resources/agent.properties"))
    private val saveAgentForTest = SaveAgent(configuration, httpClient = HttpClient(MockEngine) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json {
                serializersModule = SerializersModule {
                    // for some reason for K/N it's needed explicitly, at least for ktor 1.5.1, kotlin 1.4.21
                    contextual(HeartbeatResponse::class, HeartbeatResponse.serializer())
                }
            })
        }
        engine {
            addHandler { request ->
                when (request.url.encodedPath) {
                    "/heartbeat" -> respond(
                        Json.encodeToString(HeartbeatResponse.serializer(), ContinueResponse),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                    "/executionData" -> respond("", status = HttpStatusCode.OK)
                    "/executionLogs" -> respond("", status = HttpStatusCode.OK)
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    })

    @BeforeTest
    fun `create kexe file`() {
        platform.posix.system("touch save-0.1.0-alpha.2-linuxX64.kexe")
        platform.posix.system("echo echo 0 > save-0.1.0-alpha.2-linuxX64.kexe")
        platform.posix.system("chmod +x save-0.1.0-alpha.2-linuxX64.kexe")
    }

    @AfterTest
    fun `delete kexe file`() {
        platform.posix.system("rm -rf save-0.1.0-alpha.2-linuxX64.kexe")
    }

    @Test
    fun `agent should send heartbeats`() {
        runBlocking {
            saveAgentForTest.sendHeartbeat(ExecutionProgress(0))
        }
    }

    @Test
    fun `should change state to FINISHED after SAVE CLI returns`() = runBlocking {
        assertEquals(AgentState.IDLE, saveAgentForTest.state.value)
        runBlocking { saveAgentForTest.startSaveProcess() }
        assertEquals(AgentState.FINISHED, saveAgentForTest.state.value)
    }
}
