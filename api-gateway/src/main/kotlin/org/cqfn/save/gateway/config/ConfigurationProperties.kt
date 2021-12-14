package org.cqfn.save.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property backend properties for connection to save-backend
 * @property basicCredentials space-separated username and password for technical user to access actuator
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "gateway")
data class ConfigurationProperties(
    val backend: Backend,
    val basicCredentials: String,
) {
    /**
     * @property url URL of save-backend
     */
    data class Backend(
        val url: String,
    )
}
