/**
 * An entry point for save-demo-agent.
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.save.core.logging.logInfo
import com.saveourtool.save.demo.ServerConfiguration
import com.saveourtool.save.utils.parseConfigOrDefault

private val defaultServerConfiguration = ServerConfiguration()

fun main() {
    val serverConfiguration: ServerConfiguration = parseConfigOrDefault(defaultServerConfiguration)
    logInfo("Launching server on port ${serverConfiguration.port}")
    server(serverConfiguration).start(wait = true)
}
