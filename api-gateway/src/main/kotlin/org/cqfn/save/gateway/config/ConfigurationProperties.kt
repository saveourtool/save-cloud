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
    val oauth2: Oauth2,
) {
    /**
     * @property url URL of save-backend
     */
    data class Backend(
        val url: String,
    )

    /**
     * @property providerNameToUsernameAttribute a workaround to be able to access username attribute in unified way for all providers,
     * including spring-security built-ins
     */
    data class Oauth2(
        val providerNameToUsernameAttribute: Map<String, String>,
    )
}
