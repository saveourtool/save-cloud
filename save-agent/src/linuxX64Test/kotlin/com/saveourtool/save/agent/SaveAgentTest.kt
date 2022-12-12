package com.saveourtool.save.agent

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import platform.posix.setenv

@Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")
class SaveAgentTest : AbstractSaveAgentTest() {
    init {
        setenv(AgentEnvName.CONTAINER_ID.name, "agent-for-test", 1)
        setenv(AgentEnvName.CONTAINER_NAME.name, "save-agent-for-test", 1)
        setenv(AgentEnvName.AGENT_VERSION.name, "save-agent-version", 1)
        setenv(AgentEnvName.HEARTBEAT_URL.name, "http://localhost:5100/heartbeat", 1)
        setenv(AgentEnvName.CLI_COMMAND.name, "echo Doing nothing it test mode", 1)
        setenv(AgentEnvName.EXECUTION_ID.name, "1", 1)
    }
}
