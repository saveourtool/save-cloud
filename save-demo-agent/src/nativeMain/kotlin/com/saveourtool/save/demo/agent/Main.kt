/**
 * An entry point for save-demo-agent.
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.save.core.logging.logInfo

private val defaultServerConfiguration = ServerConfiguration()

fun main() {
    logInfo("Launching server on port ${defaultServerConfiguration.port}")
    server(defaultServerConfiguration).start(wait = true)
}
