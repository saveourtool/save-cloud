/**
 * Main entrypoint for SAVE Agent
 */

@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import kotlinx.coroutines.runBlocking

fun main() {
    val saveAgent = SaveAgent(
        // IP of your local WSL2 or whatever you use, todo: when we know how to deploy use something meaningful here
        AgentConfiguration(backendUrl = "http://172.20.51.70:5000", orchestratorUrl = "http://172.20.51.70:5100")
    )
    runBlocking {
        saveAgent.start()
    }
}
