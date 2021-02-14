package org.cqfn.save.agent

import kotlinx.coroutines.runBlocking

fun main()  {
    // IP of your local WSL2 or whatever you use, todo: when we know how to deploy use something meaningful here
    val saveAgent = SaveAgent(backendUrl = "http://172.20.51.70:3000")
    runBlocking {
        saveAgent.start()
    }
}
