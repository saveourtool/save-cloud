package org.cqfn.save.backend.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Class for properties
 *
 * @property preprocessorUrl url of preprocessor
 * @property initialBatchSize initial size of tests batch (for further scaling)
 * @property fileStorage configuration of file storage
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "backend")
data class ConfigProperties(
    val preprocessorUrl: String,
    val initialBatchSize: Int,
    val fileStorage: FileStorageConfig,
) {
    /**
     * @property location location of file storage
     */
    data class FileStorageConfig(
        val location: String,
    )
}
