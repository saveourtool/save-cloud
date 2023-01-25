package com.saveourtool.save.backend.configs

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

@Configuration
class S3Configuration {
    @Bean
    fun s3Client(): S3AsyncClient = S3AsyncClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create("admin", "12345678")
        ))
        .region(Region.AWS_ISO_GLOBAL)
        .forcePathStyle(true)
        .endpointOverride("http://localhost:9000".toHttpUrl().toUri())
        .build()
}