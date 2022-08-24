/**
 * Various utilities
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.agent.AgentConfiguration
import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.core.logging.logTrace

/**
 * @return [AgentConfiguration] with some values overridden from env variables
 */
internal fun AgentConfiguration.updateFromEnv(): AgentConfiguration {
    logTrace("Initial agent config: $this; applying overrides from env")
    return copy(
        id = requiredEnv(AgentEnvName.AGENT_ID),
        cliCommand = requiredEnv(AgentEnvName.CLI_COMMAND),
        debug = optionalEnv(AgentEnvName.DEBUG)?.toBoolean() ?: debug,
        backend = backend.copy(
            url = optionalEnv(AgentEnvName.BACKEND_URL) ?: backend.url,
        ),
        orchestratorUrl = optionalEnv(AgentEnvName.ORCHESTRATOR_URL) ?: orchestratorUrl,
        testSuitesDir = optionalEnv(AgentEnvName.TEST_SUITES_DIR) ?: testSuitesDir,
        save = save.copy(
            batchSize = optionalEnv(AgentEnvName.BATCH_SIZE)?.toInt(),
            batchSeparator = optionalEnv(AgentEnvName.BATCH_SEPARATOR),
            overrideExecCmd = optionalEnv(AgentEnvName.OVERRIDE_EXEC_CMD),
            overrideExecFlags = optionalEnv(AgentEnvName.OVERRIDE_EXEC_FLAGS),
        )
    )
}
