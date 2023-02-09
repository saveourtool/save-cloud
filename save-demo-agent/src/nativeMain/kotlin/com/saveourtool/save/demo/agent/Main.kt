/**
 * An entry point for save-demo-agent.
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.save.core.logging.logInfo
import com.saveourtool.save.utils.parseConfigOrDefault

private val defaultServerConfiguration = ServerConfiguration()

fun main() {
    logInfo("Launching server on port ${defaultServerConfiguration.port}")
    val serverConfiguration: ServerConfiguration = parseConfigOrDefault(defaultServerConfiguration)
    server(serverConfiguration).start(wait = true)
}
