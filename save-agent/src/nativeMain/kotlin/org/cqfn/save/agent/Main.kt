/**
 * Main entrypoint for SAVE Agent
 */

@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import kotlinx.coroutines.runBlocking

fun main() {
    val saveAgent = SaveAgent(
        AgentConfiguration(backendUrl = "http://host.docker.internal:5000", orchestratorUrl = "http://host.docker.internal:5100")
    )
    runBlocking {
        saveAgent.start()
    }
}
