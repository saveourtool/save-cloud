package org.cqfn.save.preprocessor.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "save")
data class ConfigProperties(
    val repository: String,
)
