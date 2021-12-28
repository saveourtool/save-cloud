/**
 * Configuration classes for save-agent
 */

package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * Configuration for save agent.
 *
 * @property id agent id
 * @property backend configuration for connection to backend
 * @property orchestratorUrl URL of SAVE orchestrator
 * @property heartbeat configuration of heartbeats
 * @property requestTimeoutMillis timeout for all http request
 * @property cliCommand a command that agent will use to run SAVE cli
 * @property debug whether debug logging should be enabled
 * @property retry configuration for HTTP request retries
 * @property logFilePath path to logs of save-cli execution
 */
@Serializable
data class AgentConfiguration(
    val id: String,
    val backend: BackendConfig,
    val orchestratorUrl: String,
    val heartbeat: HeartbeatConfig,
    val requestTimeoutMillis: Long,
    val retry: RetryConfig,
    val debug: Boolean = false,
    val cliCommand: String,
    val logFilePath: String = "logs.txt"
)

/**
 * @property intervalMillis interval between heartbeats to orchestrator in milliseconds
 */
@Serializable
data class HeartbeatConfig(
    val intervalMillis: Long,
)

/**
 * Configuration for connection to backend service
 *
 * @property url URL of backend
 * @property additionalDataEndpoint endpoint to post additional data (version etc.) to
 * @property executionDataEndpoint endpoint to post execution data to
 * @property filesEndpoint endpoint to post debug info to
 */
@Serializable
data class BackendConfig(
    val url: String,
    val additionalDataEndpoint: String = "internal/saveAgentVersion",
    val executionDataEndpoint: String = "internal/saveTestResult",
    val filesEndpoint: String = "internal/files",
)

/**
 * @property attempts number of retries when sending data
 * @property initialRetryMillis interval between successive attempts to send data
 */
@Serializable
data class RetryConfig(
    val attempts: Int,
    val initialRetryMillis: Long,
)
