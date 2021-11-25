package org.cqfn.save.gateway

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "gateway")
data class ConfigurationProperties(
    val backend: Backend,
) {
    data class Backend(
        val url: String,
    )
}