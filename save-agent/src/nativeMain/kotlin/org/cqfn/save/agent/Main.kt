/**
 * Main entrypoint for SAVE Agent
 */

@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import kotlinx.coroutines.runBlocking

fun main() {
    val saveAgent = SaveAgent(
        AgentConfiguration(backendUrl = "http://backend:5000", orchestratorUrl = "http://orchestrator:5100")
    )
    runBlocking {
        saveAgent.start()
    }
}
