package org.cqfn.save.preprocessor.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Class for properties
 *
 * @property repository path to save repository
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "save")
data class ConfigProperties(
    val repository: String,
)
