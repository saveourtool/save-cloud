/**
 * save-demo configuration
 */

package com.saveourtool.save.demo.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property fileStorage
 * @property backend
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "demo")
data class ConfigProperties(
    val fileStorage: FileStorageConfig,
    val backend: String,
)

/**
 * @property location
 */
data class FileStorageConfig(
    val location: String,
)
