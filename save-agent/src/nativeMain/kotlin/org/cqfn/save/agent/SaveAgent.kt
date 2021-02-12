package org.cqfn.save.agent

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import platform.posix.system

class SaveAgent {
    val httpClient = HttpClient()

    fun runSave(cliArgs: List<String>) {
        runBlocking {
            platform.posix.system("./save ${cliArgs.joinToString(" ")}")
        }
    }

    suspend fun sendHeartbeat() {
        httpClient.post<Heartbeat>("localhost:5100/heartbeat")  // todo url, properties file?
    }

    suspend fun sendExecutionData() {
        httpClient.post<ExecutionData>("localhost:5000/executionData")
    }
}
