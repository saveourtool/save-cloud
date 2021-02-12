package org.cqfn.save.agent

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import platform.posix.system
import kotlin.native.concurrent.AtomicReference

class SaveAgent(private val backendUrl: String = "http://localhost:5000",
                private val orchestratorUrl: String = "http://localhost:5100") {
    private val httpClient = HttpClient()
    private val state = AtomicReference(AgentState.IDLE)

    fun runSave(cliArgs: List<String>): Int {
        return platform.posix.system("./save ${cliArgs.joinToString(" ")}")
    }

    suspend fun sendHeartbeat() {
        println("Sending heartbeat to $backendUrl")
        httpClient.post<Heartbeat>("$backendUrl/heartbeat") {  // todo url, properties file?
//            this.body = Heartbeat(state.value, 0)
        }
    }

    suspend fun sendExecutionData() {
        httpClient.post<ExecutionData>("$orchestratorUrl/executionData")
    }
}
