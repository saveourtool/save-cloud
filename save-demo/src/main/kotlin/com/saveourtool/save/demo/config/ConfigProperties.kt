/**
 * save-demo configuration
 */

package com.saveourtool.save.demo.config

import com.saveourtool.save.s3.S3OperationsProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property fileStorage
 * @property s3Storage configuration of S3 storage
 * @property backend
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "demo")
data class ConfigProperties(
    val fileStorage: FileStorageConfig,
    val s3Storage: S3OperationsProperties,
    val backend: String,
)

/**
 * @property location
 */
data class FileStorageConfig(
    val location: String,
)
