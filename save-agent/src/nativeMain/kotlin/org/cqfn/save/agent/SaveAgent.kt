@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import platform.posix.system

import kotlin.native.concurrent.AtomicReference
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * A main class for SAVE Agent
 */
class SaveAgent(private val backendUrl: String = "http://localhost:5000",
                private val orchestratorUrl: String = "http://localhost:5100",
                private val httpClient: HttpClient = HttpClient {
                    install(JsonFeature) {
                        serializer = KotlinxSerializer(Json {
                            serializersModule = SerializersModule {
                                // for some reason for K/N it's needed explicitly, at least for ktor 1.5.1, kotlin 1.4.21
                                contextual(HeartbeatResponse::class, HeartbeatResponse.serializer())
                            }
                        })
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 1000
                    }
                }
) {
    /**
     * The current [AgentState] of this agent
     */
    val state = AtomicReference(AgentState.IDLE)
    private val isStopped = atomic(false)
    private lateinit var saveProcessJob: Job

    /**
     * @return Unit
     */
    suspend fun start() = coroutineScope {
        println("Starting agent")
        val heartbeatsJob = launch { startHeartbeats() }
        heartbeatsJob.join()
    }

    /**
     * Shutdown the Agent
     */
    fun stop() {
        isStopped.getAndSet(true)
    }

    private suspend fun startHeartbeats() = coroutineScope {
        println("Scheduling heartbeats")
        while (isStopped.value.not()) {
            val deferred = async { sendHeartbeat() }
            try {
                when (deferred.await()) {
                    is NewJobResponse -> {
                        require(::saveProcessJob.isInitialized.not() || saveProcessJob.isCompleted) {
                            "Shouldn't start new process when there is the previous running"
                        }
                        saveProcessJob = launch {
                            // new job received from Orchestrator, spawning SAVE CLI process
                            startSaveProcess()
                        }
                    }
                    TerminatingResponse -> stop()
                    EmptyResponse -> Unit  // do nothing
                    else -> {
                        // this is a generated else block
                    }
                }
            } catch (e: Exception) {
                println("Exception during heartbeat: ${e.message}")
            }
            // todo: start waiting after request was sent, not after response?
            println("Waiting for 15 sec")
            delay(15_000)
        }
    }

    /**
     * @return Unit
     */
    internal suspend fun startSaveProcess() = coroutineScope {
        // blocking execution of OS process
        // val code = saveAgent.runSave(emptyList())
        state.value = AgentState.FINISHED
        val deferred = async {
            // todo: read data from files here
            sendExecutionData()
        }
        deferred.await()
        state.value = AgentState.IDLE
    }

    private fun runSave(cliArgs: List<String>) = platform.posix.system("./save ${cliArgs.joinToString(" ")}")

    /**
     * @return a [HeartbeatResponse] from Orchestrator
     */
    internal suspend fun sendHeartbeat(): HeartbeatResponse {
        println("Sending heartbeat to $backendUrl")
        // if current state is IDLE or FINISHED, should accept new jobs as a response
        return httpClient.post("$backendUrl/heartbeat") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = Heartbeat(state.value, 0)
        }
    }

    private suspend fun sendExecutionData() {
        httpClient.post<Unit>("$orchestratorUrl/executionData") {
            contentType(ContentType.Application.Json)
            body = ExecutionData(emptyList())
        }
    }
}
