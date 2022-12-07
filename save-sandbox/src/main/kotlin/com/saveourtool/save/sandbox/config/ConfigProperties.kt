package com.saveourtool.save.sandbox.config

import com.saveourtool.save.service.LokiConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property fileStorage configuration of file storage
 * @property agentSettings properties for save-agents
 * @property lokiConfig config of loki service for logging
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "sandbox")
data class ConfigProperties(
    val fileStorage: FileStorageConfig,
    val agentSettings: AgentSettings,
    val lokiConfig: LokiConfig? = null,
) {
    /**
     * @property location location of file storage
     */
    data class FileStorageConfig(
        val location: String,
    )

    /**
     * @property sandboxUrl the URL of save-sandbox that will be reported to save-agents.
     */
    data class AgentSettings(
        val sandboxUrl: String,
    )
}
