package org.cqfn.save.preprocessor.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Class for properties
 *
 * @property repository path to save repository
 * @property backend
 * @property orchestrator
 * @property reposFileName path to the file with standard repositories
 * @property reposTokenFileName path to the file with git username and token for standard repositories
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "save")
data class ConfigProperties(
    val repository: String,
    val backend: String,
    val orchestrator: String,
    val reposFileName: String,
    val reposTokenFileName: String,
)
