/**
 * An entry point for save-demo-agent.
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.save.demo.agent.utils.parseConfig
import com.saveourtool.save.demo.agent.utils.setupEnvironment

fun main() {
    val config = parseConfig()
    setupEnvironment(config.demoUrl, config.demoConfiguration)
    server(config.serverConfiguration).start()
}
