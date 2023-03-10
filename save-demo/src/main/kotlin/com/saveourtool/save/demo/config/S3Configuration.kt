package com.saveourtool.save.demo.config

import com.saveourtool.save.s3.DefaultS3Operations
import com.saveourtool.save.s3.S3Operations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for S3
 */
@Configuration
class S3Configuration(
    private val configProperties: ConfigProperties,
) {
    /**
     * @return [S3Operations] as a Spring's bean
     */
    @Bean
    fun s3Operations(): S3Operations = DefaultS3Operations(configProperties.s3Storage)

    /**
     * @return [S3Operations] from container as a Spring's bean
     */
    @Bean
    fun s3OperationsFromContainer(): S3Operations = DefaultS3Operations(configProperties.s3Storage.copy(
        endpoint = configProperties.s3Storage.endpointFromContainer,
        createBucketIfNotExists = false,
    ))
}
