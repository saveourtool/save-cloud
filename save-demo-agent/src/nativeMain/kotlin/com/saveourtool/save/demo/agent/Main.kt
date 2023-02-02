/**
 * An entry point for save-demo-agent.
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.save.demo.agent.utils.setupEnvironment
import com.saveourtool.save.utils.parseConfig

fun main() {
    val config: DemoAgentConfig = parseConfig()
    setupEnvironment(config.demoUrl, config.demoConfiguration)
    server(config.serverConfiguration).start()
}
