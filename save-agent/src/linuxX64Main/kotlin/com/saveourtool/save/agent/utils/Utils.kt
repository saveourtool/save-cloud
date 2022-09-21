/**
 * Various utilities
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.agent.AgentConfiguration
import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.agent.SaveCliOverrides
import com.saveourtool.save.core.logging.logTrace
import generated.SAVE_CORE_VERSION

internal const val SAVE_CLI_EXECUTABLE_NAME = "save-$SAVE_CORE_VERSION-linuxX64.kexe"
internal const val TEST_SUITES_DIR_NAME = "test-suites"

/**
 * @return [AgentConfiguration] with some values overridden from env variables
 */
internal fun AgentConfiguration.updateFromEnv(): AgentConfiguration {
    logTrace("Initial agent config: $this; applying overrides from env")
    return copy(
        id = optionalEnv(AgentEnvName.AGENT_ID) ?: id,
        cliCommand = optionalEnv(AgentEnvName.CLI_COMMAND) ?: cliCommand,
        debug = optionalEnv(AgentEnvName.DEBUG)?.toBoolean() ?: debug,
        backend = backend.copy(
            url = optionalEnv(AgentEnvName.BACKEND_URL) ?: backend.url,
        ),
        orchestrator = orchestrator.copy(
            url = optionalEnv(AgentEnvName.ORCHESTRATOR_URL) ?: orchestrator.url,
        ),
        testSuitesDir = optionalEnv(AgentEnvName.TEST_SUITES_DIR) ?: testSuitesDir,
        saveCliOverrides = SaveCliOverrides(
            batchSize = optionalEnv(AgentEnvName.BATCH_SIZE)?.toInt(),
            batchSeparator = optionalEnv(AgentEnvName.BATCH_SEPARATOR),
            overrideExecCmd = optionalEnv(AgentEnvName.OVERRIDE_EXEC_CMD),
            overrideExecFlags = optionalEnv(AgentEnvName.OVERRIDE_EXEC_FLAGS),
        )
    )
}
