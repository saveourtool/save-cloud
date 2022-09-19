/**
 * Configuration classes for save-agent
 */

package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.SAVE_CLI_EXECUTABLE_NAME
import com.saveourtool.save.agent.utils.TEST_SUITES_DIR_NAME
import com.saveourtool.save.agent.utils.requiredEnv
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.config.OutputStreamType
import com.saveourtool.save.core.config.ReportType

import kotlinx.serialization.Serializable

/**
 * Configuration for save agent.
 *
 * @property id agent id
 * @property backend configuration for connection to backend
 * @property orchestrator configuration for connection to orchestrator
 * @property cliCommand a command that agent will use to run SAVE cli
 * @property heartbeat configuration of heartbeats
 * @property requestTimeoutMillis timeout for all http request
 * @property retry configuration for HTTP request retries
 * @property debug whether debug logging should be enabled
 * @property testSuitesDir directory where tests and additional files need to be stored into
 * @property logFilePath path to logs of save-cli execution
 * @property save additional configuration for save-cli
 */
@Serializable
data class AgentConfiguration(
    val id: String,
    val backend: BackendConfig,
    val orchestrator: OrchestratorConfig,
    val cliCommand: String = "./$SAVE_CLI_EXECUTABLE_NAME",
    val heartbeat: HeartbeatConfig = HeartbeatConfig(),
    val requestTimeoutMillis: Long = 60000,
    val retry: RetryConfig = RetryConfig(),
    val debug: Boolean = false,
    val testSuitesDir: String = TEST_SUITES_DIR_NAME,
    val logFilePath: String = "logs.txt",
    val save: SaveCliConfig = SaveCliConfig(),
) {
    companion object {
        /**
         * @return [AgentConfiguration] with required fields initialized from env
         */
        internal fun initializeFromEnv() = AgentConfiguration(
            id = requiredEnv(AgentEnvName.AGENT_ID),
            backend = BackendConfig(
                url = requiredEnv(AgentEnvName.BACKEND_URL),
            ),
            orchestrator = OrchestratorConfig(
                url = requiredEnv(AgentEnvName.ORCHESTRATOR_URL),
            ),
        )
    }
}

/**
 * @property intervalMillis interval between heartbeats to orchestrator in milliseconds
 */
@Serializable
data class HeartbeatConfig(
    val intervalMillis: Long = 15000,
)

/**
 * Configuration for connection to orchestrator service
 *
 * @property url URL of orchestrator
 * @property heartbeatEndpoint endpoint to post heartbeats to
 * @property executionLogsEndpoint endpoint to post executionLogs to
 */
@Serializable
data class OrchestratorConfig(
    val url: String,
    val heartbeatEndpoint: String = "/heartbeat",
    val executionLogsEndpoint: String = "/executionLogs",
)

/**
 * Configuration for connection to backend service
 *
 * @property url URL of backend
 * @property additionalDataEndpoint endpoint to post additional data (version etc.) to
 * @property executionDataEndpoint endpoint to post execution data to
 * @property debugInfoEndpoint endpoint to post debug info to
 * @property fileEndpoint endpoint to download files from
 * @property testSourceSnapshotEndpoint endpoint to download test source snapshots from
 * @property saveCliDownloadEndpoint endpoint to download save-cli binary from
 */
@Serializable
data class BackendConfig(
    val url: String,
    val additionalDataEndpoint: String = "/internal/saveAgentVersion",
    val executionDataEndpoint: String = "/internal/saveTestResult",
    val fileEndpoint: String = "/internal/files/download",
    val testSourceSnapshotEndpoint: String = "/internal/test-suites-sources/download-snapshot-by-execution-id",
    val saveCliDownloadEndpoint: String = "/internal/files/download-save-cli",
    val debugInfoEndpoint: String = "/internal/files/debug-info",
)

/**
 * @property attempts number of retries when sending data
 * @property initialRetryMillis interval between successive attempts to send data
 */
@Serializable
data class RetryConfig(
    val attempts: Int = 5,
    val initialRetryMillis: Long = 2000,
)

/**
 * @property reportType corresponds to flag `--report-type` of save-cli
 * @property reportDir corresponds to flag `--report-dir` of save-cli
 * @property logType corresponds to flag `--log` of save-cli
 * @property resultOutput corresponds to flag `--result-output` of save-cli
 * @property batchSize corresponds to flag `--batch-size` of save-cli (optional)
 * @property batchSeparator corresponds to flag `--batch-separator` of save-cli (optional)
 * @property overrideExecCmd corresponds to flag `--override-exec-cmd` of save-cli (optional)
 * @property overrideExecFlags corresponds to flag `--override-exec-flags` of save-cli (optional)
 */
@Serializable
data class SaveCliConfig(
    val reportType: ReportType = ReportType.JSON,
    val resultOutput: OutputStreamType = OutputStreamType.FILE,
    val reportDir: String = "save-reports",
    val logType: LogType = LogType.ALL,
    val batchSize: Int? = null,
    val batchSeparator: String? = null,
    val overrideExecCmd: String? = null,
    val overrideExecFlags: String? = null,
)
