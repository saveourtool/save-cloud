package com.saveourtool.save.cosv.configs

import com.saveourtool.save.s3.S3OperationsProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path

/**
 * Class for properties
 *
 * @property s3Storage configuration of S3 storage
 * @property workingDir a local folder for tmp files
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "cosv")
data class ConfigProperties(
    override val s3Storage: S3OperationsProperties,
    val workingDir: Path,
) : S3OperationsProperties.Provider
