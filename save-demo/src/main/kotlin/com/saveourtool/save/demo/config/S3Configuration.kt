package com.saveourtool.save.demo.config

import com.saveourtool.save.s3.DefaultS3OperationsProjectReactor
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
    fun s3Operations(): S3Operations = DefaultS3OperationsProjectReactor(configProperties.s3Storage)
}
