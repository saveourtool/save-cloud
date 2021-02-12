package org.cqfn.save.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("Creating agent")
    val saveAgent = SaveAgent(backendUrl = "http://172.20.51.70:3000")
    println("Starting agent")
    val saveProcessJob = launch {
//        val code = saveAgent.runSave(emptyList())
    }
    val heartbeatsJob = launch {
        println("Scheduling heartbeats")
        while (true) {
            saveAgent.sendHeartbeat()
            println("Waiting for 15 sec")
            delay(15_000)
        }
    }
    heartbeatsJob.join()
}
