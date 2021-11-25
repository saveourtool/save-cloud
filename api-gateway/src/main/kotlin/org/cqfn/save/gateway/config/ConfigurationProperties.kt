package org.cqfn.save.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property backend properties for connection to save-backend
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "gateway")
data class ConfigurationProperties(
    val backend: Backend,
) {
    /**
     * @property url URL of save-backend
     */
    data class Backend(
        val url: String,
    )
}
