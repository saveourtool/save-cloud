package org.cqfn.save.preprocessor.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Class for properties
 *
 * @property repository path to save repository
 * @property backend
 * @property orchestrator
 * @property executionLimit
 * @property standardTestRepository
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "save")
data class ConfigProperties(
    val repository: String,
    val backend: String,
    val orchestrator: String,
    val executionLimit: Int,
    val standardTestRepository: String,
)
