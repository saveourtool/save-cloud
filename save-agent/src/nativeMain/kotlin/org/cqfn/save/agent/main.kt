/**
 * Main entrypoint for SAVE Agent
 */

@file:Suppress("FILE_NAME_INCORRECT", "PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import kotlinx.coroutines.runBlocking

fun main() {
    // IP of your local WSL2 or whatever you use, todo: when we know how to deploy use something meaningful here
    val saveAgent = SaveAgent(backendUrl = "http://172.20.51.70:5000", orchestratorUrl = "http://172.20.51.70:5100")
    runBlocking {
        saveAgent.start()
    }
}
