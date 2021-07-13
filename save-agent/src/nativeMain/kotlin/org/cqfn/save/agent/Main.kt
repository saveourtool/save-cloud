/**
 * Main entrypoint for SAVE Agent
 */

package org.cqfn.save.agent

import org.cqfn.save.agent.utils.readProperties
import org.cqfn.save.core.logging.isDebugEnabled
import org.cqfn.save.core.logging.logDebug

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val config: AgentConfiguration = Properties.decodeFromStringMap(
        readProperties("agent.properties")
    )
    isDebugEnabled = config.debug
    logDebug("Instantiating save-agent with config $config")
    val saveAgent = SaveAgent(config)
    runBlocking {
        saveAgent.start()
    }
}
