package com.saveourtool.save.s3

import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import java.net.URI
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * @property endpoint S3 endpoint (URI)
 * @property presignedEndpoint S3 endpoint (URI) for pre-signed requests, it's equal to [endpoint] by default
 * @property bucketName bucket name for all S3 storages
 * @property prefix a common prefix for all S3 storages
 * @property credentials credentials to S3
 * @property httpClient configuration for http client to S3
 * @property async configuration for async operations
 */
data class S3OperationsProperties(
    val endpoint: URI,
    val presignedEndpoint: URI = endpoint,
    val bucketName: String,
    val prefix: String = "",
    val credentials: CredentialsProperties,
    val httpClient: HttpClientProperties = HttpClientProperties(),
    val async: AsyncProperties = AsyncProperties(),
) {
    /**
     * @property accessKeyId [AwsCredentials.accessKeyId]
     * @property secretAccessKey [AwsCredentials.secretAccessKey]
     */
    data class CredentialsProperties(
        val accessKeyId: String,
        val secretAccessKey: String,
    ) {
        /**
         * @return [AwsCredentials] created from this object
         */
        fun toAwsCredentials(): AwsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)
    }

    /**
     * @property maxConcurrency
     * @property connectionTimeout
     * @property connectionAcquisitionTimeout
     */
    data class HttpClientProperties(
        val maxConcurrency: Int = 5,
        val connectionTimeout: Duration = 30.seconds.toJavaDuration(),
        val connectionAcquisitionTimeout: Duration = 1.minutes.toJavaDuration(),
    )

    /**
     * @property minPoolSize
     * @property maxPoolSize
     * @property queueSize
     * @property ttl time to live, default value took from [reactor.core.scheduler.BoundedElasticScheduler.DEFAULT_TTL_SECONDS]
     */
    data class AsyncProperties(
        val minPoolSize: Int = Schedulers.DEFAULT_POOL_SIZE,
        val maxPoolSize: Int = Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
        val queueSize: Int = Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
        val ttl: Duration = 60.seconds.toJavaDuration(),
    )
}
