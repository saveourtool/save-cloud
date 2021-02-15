@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import com.benasher44.uuid.uuid4
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
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
@OptIn(KtorExperimentalAPI::class)
class SaveAgent(private val config: AgentConfiguration,
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
                        requestTimeoutMillis = config.requestTimeoutMillis
                    }
                }
) {
    private val id = uuid4().toString()
    /**
     * The current [AgentState] of this agent
     */
    val state = AtomicReference(AgentState.IDLE)
    private val isStopped = atomic(false)
    private var saveProcessJob: Job? = null

    /**
     * @return Unit
     */
    suspend fun start() = coroutineScope {
        println("Starting agent")
        val heartbeatsJob = launch { startHeartbeats() }
        heartbeatsJob.join()
        println("Heartbeats job is done")
    }

    /**
     * Shutdown the Agent
     */
    fun stop() {
        println("Stopping agent")
        httpClient.close()
        if (saveProcessJob?.isActive == true) saveProcessJob?.cancel()
        isStopped.getAndSet(true)
    }

    private suspend fun startHeartbeats() = coroutineScope {
        println("Scheduling heartbeats")
        while (isStopped.value.not()) {
            val deferred = async { sendHeartbeat() }
            try {
                when (deferred.await()) {
                    is NewJobResponse -> {
                        require(saveProcessJob == null || saveProcessJob?.isCompleted == true) {
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
            println("Waiting for ${config.heartbeatIntervalMillis} sec")
            delay(config.heartbeatIntervalMillis)
        }
    }

    /**
     * @return Unit
     */
    internal suspend fun startSaveProcess() = coroutineScope {
        // blocking execution of OS process
        // val code = saveAgent.runSave(emptyList())
        state.value = AgentState.FINISHED
        // todo: read data from files here
        sendExecutionData(ExecutionData(emptyList()))
        state.value = AgentState.IDLE
    }

    private fun runSave(cliArgs: List<String>) = platform.posix.system("./save ${cliArgs.joinToString(" ")}")

    /**
     * @return a [HeartbeatResponse] from Orchestrator
     */
    internal suspend fun sendHeartbeat(): HeartbeatResponse {
        // log.trace("Sending heartbeat to $orchestratorUrl")
        // if current state is IDLE or FINISHED, should accept new jobs as a response
        return httpClient.post("${config.orchestratorUrl}/heartbeat") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = Heartbeat(id, state.value, 0)
        }
    }

    private suspend fun sendExecutionData(executionData: ExecutionData): Unit = coroutineScope {
        var result = runCatching {
            postExecutionData(executionData)
        }
        while (result.isFailure) {
            println("Backend is unreachable, will retry in ${config.executionDataRequestRetryMillis} second. Reason:  ${result.exceptionOrNull()?.message}")
            delay(config.executionDataRequestRetryMillis)
            result = runCatching {
                postExecutionData(executionData)
            }
        }
    }

    private suspend fun postExecutionData(executionData: ExecutionData) {
        httpClient.post<Unit>("${config.backendUrl}/executionData") {
            contentType(ContentType.Application.Json)
            body = executionData
        }
    }
}
