/**
 * Various utilities
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.agent.AgentConfiguration
import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.core.logging.logTrace

internal const val SAVE_CLI_EXECUTABLE_NAME = "save-linuxX64.kexe"
internal const val TEST_SUITES_DIR_NAME = "test-suites"

/**
 * @return [AgentConfiguration] with some values overridden from env variables
 */
internal fun AgentConfiguration.updateFromEnv(): AgentConfiguration {
    logTrace("Initial agent config: $this; applying overrides from env")
    return copy(
        id = optionalEnv(AgentEnvName.AGENT_ID) ?: id,
        name = optionalEnv(AgentEnvName.AGENT_NAME) ?: name,
        version = optionalEnv(AgentEnvName.AGENT_VERSION) ?: version,
        heartbeat = heartbeat.copy(
            url = optionalEnv(AgentEnvName.HEARTBEAT_URL) ?: heartbeat.url,
        ),
        cliCommand = optionalEnv(AgentEnvName.CLI_COMMAND) ?: cliCommand,
        debug = optionalEnv(AgentEnvName.DEBUG)?.toBoolean() ?: debug,
        testSuitesDir = optionalEnv(AgentEnvName.TEST_SUITES_DIR) ?: testSuitesDir,
    )
}
