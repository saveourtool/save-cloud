/**
 * save-demo configuration
 */

package com.saveourtool.save.demo.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property fileStorage
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "demo")
data class ConfigProperties(
    val fileStorage: FileStorageConfig,
)

/**
 * @property location
 */
data class FileStorageConfig(
    val location: String,
)
