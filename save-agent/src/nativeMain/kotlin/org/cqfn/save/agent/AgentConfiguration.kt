@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

/**
 * Configuration for save agent
 *
 * @property backendUrl URL of SAVE backend
 * @property orchestratorUrl URL of SAVE orchestrator
 * @property heartbeatIntervalMillis interval between heartbeats to orchestrator
 * @property requestTimeoutMillis timeout for all http request
 * @property executionDataRequestRetryMillis interval between successive attempts to send execution data
 */
data class AgentConfiguration(
    val backendUrl: String = "http://localhost:5000",
    val orchestratorUrl: String = "http://localhost:5100",
    val heartbeatIntervalMillis: Long = 15_000L,
    val requestTimeoutMillis: Long = 1_000L,
    val executionDataRequestRetryMillis: Long = 1_000L,
)
