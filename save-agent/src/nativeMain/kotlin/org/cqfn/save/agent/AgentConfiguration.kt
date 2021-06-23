/**
 * Configuration classes for save-agent
 */

@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import generated.SAVE_CORE_VERSION
import kotlinx.serialization.Serializable

/**
 * Configuration for save agent.
 *
 * @property id agent id
 * @property backendUrl URL of SAVE backend
 * @property orchestratorUrl URL of SAVE orchestrator
 * @property heartbeat configuration of heartbeats
 * @property requestTimeoutMillis timeout for all http request
 * @property executionDataRetryAttempts number of retries when sending execution data
 * @property executionDataInitialRetryMillis interval between successive attempts to send execution data
 * @property cliCommand a command that agent will use to run SAVE cli
 */
@Serializable
data class AgentConfiguration(
    val id: String,
    val backendUrl: String,
    val orchestratorUrl: String,
    val heartbeat: HeartbeatConfig,
    val requestTimeoutMillis: Long,
    val executionDataRetryAttempts: Int,
    val executionDataInitialRetryMillis: Long,
    val cliCommand: String = "test -f run.sh && ./run.sh; ./save-$SAVE_CORE_VERSION-linuxX64.kexe",
)

/**
 * @property interval interval between heartbeats to orchestrator in milliseconds
 */
@Serializable
data class HeartbeatConfig(
    val interval: Long,
)
