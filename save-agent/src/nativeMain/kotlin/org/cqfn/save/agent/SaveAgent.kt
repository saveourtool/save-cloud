@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import com.benasher44.uuid.uuid4
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import platform.posix.system

import kotlin.native.concurrent.AtomicReference
import kotlinx.coroutines.Job
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
    /**
     * The current [AgentState] of this agent
     */
    val state = AtomicReference(AgentState.IDLE)
    private val id = uuid4().toString()
    private var saveProcessJob: Job? = null

    /**
     * @return Unit
     */
    suspend fun start() = coroutineScope {
        println("Starting agent")
        val heartbeatsJob = launch { startHeartbeats() }
        heartbeatsJob.join()
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // when with sealed class
    private suspend fun startHeartbeats() = coroutineScope {
        println("Scheduling heartbeats")
        while (true) {
            val response = runCatching {
                // TODO: get execution progress here
                sendHeartbeat(ExecutionProgress(0))
            }
            if (response.isSuccess) {
                when (response.getOrNull()) {
                    is NewJobResponse -> maybeStartSaveProcess()
                    WaitResponse -> state.value = AgentState.IDLE
                    ContinueResponse -> Unit  // do nothing
                }
            } else {
                println("Exception during heartbeat: ${response.exceptionOrNull()?.message}")
            }
            // todo: start waiting after request was sent, not after response?
            println("Waiting for ${config.heartbeatIntervalMillis} sec")
            delay(config.heartbeatIntervalMillis)
        }
    }

    private suspend fun maybeStartSaveProcess() = coroutineScope {
        if (saveProcessJob?.isCompleted == false) {
            println("Shouldn't start new process when there is the previous running")
        } else {
            saveProcessJob = launch {
                // new job received from Orchestrator, spawning SAVE CLI process
                startSaveProcess()
            }
        }
    }

    /**
     * @return Unit
     */
    internal suspend fun startSaveProcess() = coroutineScope {
        // blocking execution of OS process
        // val code = saveAgent.runSave(emptyList())
        // todo: read data from files here
        sendExecutionData(ExecutionData(emptyList()))
        state.value = AgentState.FINISHED
    }

    private fun runSave(cliArgs: List<String>) = platform.posix.system("./save ${cliArgs.joinToString(" ")}")

    /**
     * @param executionProgress execution progress that will be sent in a heartbeat message
     * @return a [HeartbeatResponse] from Orchestrator
     */
    internal suspend fun sendHeartbeat(executionProgress: ExecutionProgress): HeartbeatResponse {
        // log.trace("Sending heartbeat to $orchestratorUrl")
        // if current state is IDLE or FINISHED, should accept new jobs as a response
        return httpClient.post("${config.orchestratorUrl}/heartbeat") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = Heartbeat(id, state.value, executionProgress)
        }
    }

    /**
     * Attempt to send execution data to backend, will retry several times, while increasing delay 2 times on each iteration.
     */
    private suspend fun sendExecutionData(executionData: ExecutionData): Unit = coroutineScope {
        var retryInterval = config.executionDataInitialRetryMillis
        repeat(config.executionDataRetryAttempts) { attempt ->
            val result = runCatching {
                postExecutionData(executionData)
            }
            if (result.isSuccess) {
                return@repeat
            } else {
                println("Backend is unreachable (x$attempt), will retry in $retryInterval second. Reason:  ${result.exceptionOrNull()?.message}")
                delay(retryInterval)
                retryInterval *= 2
            }
        }
    }

    private suspend fun postExecutionData(executionData: ExecutionData) {
        httpClient.post<Unit> {
            url("${config.backendUrl}/executionData")
            contentType(ContentType.Application.Json)
            body = executionData
        }
    }
}
