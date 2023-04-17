package com.saveourtool.save.sandbox.config

import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.service.LokiConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property s3Storage configuration of S3 storage
 * @property agentSettings properties for save-agents
 * @property lokiConfig config of loki service for logging
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "sandbox")
data class ConfigProperties(
    override val s3Storage: S3OperationsProperties,
    val agentSettings: AgentSettings,
    val lokiConfig: LokiConfig? = null,
) : S3OperationsProperties.Provider {
    /**
     * @property sandboxUrl the URL of save-sandbox that will be reported to save-agents.
     */
    data class AgentSettings(
        val sandboxUrl: String,
    )
}
