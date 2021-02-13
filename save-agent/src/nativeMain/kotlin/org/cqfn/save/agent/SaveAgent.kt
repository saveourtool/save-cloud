package org.cqfn.save.agent

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.posix.system
import kotlin.native.concurrent.AtomicReference

class SaveAgent(private val backendUrl: String = "http://localhost:5000",
                private val orchestratorUrl: String = "http://localhost:5100",
                private val httpClient: HttpClient = HttpClient {
                    install(JsonFeature)
                    install(HttpTimeout) {
                        requestTimeoutMillis = 1000
                    }
                }
) {
    val state = AtomicReference(AgentState.IDLE)
    private val isStopped = atomic(false)

    suspend fun start() = coroutineScope {
        println("Starting agent")
        val saveProcessJob = launch {
//        val code = saveAgent.runSave(emptyList())
        }
        saveProcessJob.invokeOnCompletion {
            state.value = AgentState.FINISHED
        }
        val heartbeatsJob = launch {
            println("Scheduling heartbeats")
            while (isStopped.value.not()) {
                val deferred = async { sendHeartbeat() }
                try {
                    deferred.await()
                } catch (e: Exception) {
                    println("Exception during heartbeat: ${e.message}")
                }
                println("Waiting for 15 sec")
                delay(15_000)
            }
        }
        heartbeatsJob.join()
    }

    fun stop() {
        isStopped.getAndSet(true)
    }

    fun runSave(cliArgs: List<String>): Int {
        return platform.posix.system("./save ${cliArgs.joinToString(" ")}")
    }

    suspend fun sendHeartbeat() {
        println("Sending heartbeat to $backendUrl")
        httpClient.post<Unit>("$backendUrl/heartbeat") {
            contentType(ContentType.Application.Json)
            body = Heartbeat(state.value, 0)
        }
    }

    suspend fun sendExecutionData() {
        httpClient.post<ExecutionData>("$orchestratorUrl/executionData")
    }
}
