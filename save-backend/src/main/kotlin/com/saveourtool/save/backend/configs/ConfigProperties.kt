package com.saveourtool.save.backend.configs

import com.saveourtool.save.service.LokiConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import java.net.URI
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Class for properties
 *
 * @property preprocessorUrl url of preprocessor
 * @property orchestratorUrl url of save-orchestrator
 * @property demoUrl url of save-demo
 * @property initialBatchSize initial size of tests batch (for further scaling)
 * @property fileStorage configuration of file storage
 * @property fileStorage configuration of S3 storage
 * @property scheduling configuration for scheduled tasks
 * @property agentSettings properties for save-agents
 * @property testAnalysisSettings properties of the flaky test detector.
 * @property loki config of loki service for logging
 * @property s3Storage
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "backend")
data class ConfigProperties(
    val preprocessorUrl: String,
    val orchestratorUrl: String,
    val demoUrl: String,
    val initialBatchSize: Int,
    val fileStorage: FileStorageConfig,
    val s3Storage: S3StorageConfig,
    val scheduling: Scheduling = Scheduling(),
    val agentSettings: AgentSettings = AgentSettings(),
    val testAnalysisSettings: TestAnalysisSettings = TestAnalysisSettings(),
    val loki: LokiConfig? = null,
) {
    /**
     * @property location location of file storage
     */
    data class FileStorageConfig(
        val location: String,
    )

    /**
     * @property endpoint S3 endpoint (URI)
     * @property bucketName bucket name for all S3 storages
     * @property prefix a common prefix for all S3 storages
     * @property credentials credentials to S3
     * @property httpClient configuration for http client to S3
     */
    data class S3StorageConfig(
        val endpoint: URI,
        val bucketName: String,
        val prefix: String = "",
        val credentials: S3Credentials,
        val httpClient: S3HttpClientConfig = S3HttpClientConfig(),
    )

    /**
     * @property accessKeyId [AwsCredentials.accessKeyId]
     * @property secretAccessKey [AwsCredentials.secretAccessKey]
     */
    data class S3Credentials(
        val accessKeyId: String,
        val secretAccessKey: String,
    ) {
        /**
         * @return [AwsCredentials] created from this object
         */
        fun toAwsCredentials(): AwsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)
    }

    /**
     * @property maxConcurrency
     * @property connectionTimeout
     * @property connectionAcquisitionTimeout
     */
    data class S3HttpClientConfig(
        val maxConcurrency: Int = 5,
        val connectionTimeout: Duration = 30.seconds.toJavaDuration(),
        val connectionAcquisitionTimeout: Duration = 1.minutes.toJavaDuration(),
    )

    /**
     * @property standardSuitesUpdateCron cron expression to schedule update of standard test suites (by default, every hour)
     * @property baseImagesBuildCron cron expression to schedule builds of base docker images for test executions on all supported SDKs
     */
    data class Scheduling(
        val standardSuitesUpdateCron: String = "0 0 */1 * * ?",
        val baseImagesBuildCron: String = "0 0 */1 * * ?",
    )

    /**
     * @property backendUrl the URL of save-backend that will be reported to
     *   save-agents.
     */
    data class AgentSettings(
        val backendUrl: String = DEFAULT_BACKEND_URL,
    )

    /**
     * @property slidingWindowSize the size of the sliding window (the maximum
     *   sample size preserved in memory for any given test).
     * @property parallelStartup whether historical data should be read in
     *   parallel.
     * @property replayOnStartup enables [com.saveourtool.save.backend.service.TestAnalysisService.replayHistoricalData] on startup
     */
    data class TestAnalysisSettings(
        val slidingWindowSize: Int = DEFAULT_SLIDING_WINDOW_SIZE,
        val parallelStartup: Boolean = true,
        val replayOnStartup: Boolean = true,
    )

    private companion object {
        private const val DEFAULT_BACKEND_URL = "http://backend:5800"
        private const val DEFAULT_SLIDING_WINDOW_SIZE = Int.MAX_VALUE
    }
}
