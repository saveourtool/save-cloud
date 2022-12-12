package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.fs
import com.saveourtool.save.agent.utils.updateFromEnv
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.logging.logType
import com.saveourtool.save.reporter.Report
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.io.path.*

@Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")
class SaveAgentJvmTest : AbstractSaveAgentTest() {
    init {
        System.setProperty(AgentEnvName.CONTAINER_ID.name, "agent-for-test")
        System.setProperty(AgentEnvName.CONTAINER_NAME.name, "save-agent-for-test")
        System.setProperty(AgentEnvName.AGENT_VERSION.name, "save-agent-version")
        System.setProperty(AgentEnvName.HEARTBEAT_URL.name, "http://localhost:5100/heartbeat")
        System.setProperty(AgentEnvName.CLI_COMMAND.name, "echo Doing nothing it test mode")
        System.setProperty(AgentEnvName.EXECUTION_ID.name, "1")
        System.setProperty(AgentEnvName.DEBUG.name, "true")
    }
}
