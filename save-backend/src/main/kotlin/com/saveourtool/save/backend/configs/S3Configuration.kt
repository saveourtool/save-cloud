package com.saveourtool.save.backend.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

/**
 * Configuration for S3
 */
@Configuration
class S3Configuration(
    private val configProperties: ConfigProperties,
) {
    /**
     * @return [S3AsyncClient] as a Spring's bean
     */
    @Bean
    fun s3Client(): S3AsyncClient = with(configProperties.s3Storage) {
        S3AsyncClient.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    credentials.toAwsCredentials()
                )
            )
            .httpClientBuilder(
                NettyNioAsyncHttpClient.builder()
                    .maxConcurrency(httpClient.maxConcurrency)
                    .connectionTimeout(httpClient.connectionTimeout)
                    .connectionAcquisitionTimeout(httpClient.connectionAcquisitionTimeout)
            )
            .region(Region.AWS_ISO_GLOBAL)
            .forcePathStyle(true)
            .endpointOverride(endpoint)
            .build()
    }
}
