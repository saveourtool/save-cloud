package com.saveourtool.save.backend.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Class for properties
 *
 * @property preprocessorUrl url of preprocessor
 * @property initialBatchSize initial size of tests batch (for further scaling)
 * @property fileStorage configuration of file storage
 * @property orchestratorUrl url of save-orchestrator
 * @property scheduling configuration for scheduled tasks
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "backend")
data class ConfigProperties(
    val preprocessorUrl: String,
    val orchestratorUrl: String,
    val initialBatchSize: Int,
    val fileStorage: FileStorageConfig,
    val scheduling: Scheduling = Scheduling(),
) {
    /**
     * @property location location of file storage
     */
    data class FileStorageConfig(
        val location: String,
    )

    /**
     * @property standardSuitesUpdateCron cron expression to schedule update of standard test suites (by default, every hour)
     * @property baseImagesBuildCron cron expression to schedule builds of base docker images for test executions on all supported SDKs
     */
    data class Scheduling(
        val standardSuitesUpdateCron: String = "0 0 */1 * * ?",
        val baseImagesBuildCron: String = "0 0 */1 * * ?",
    )
}
