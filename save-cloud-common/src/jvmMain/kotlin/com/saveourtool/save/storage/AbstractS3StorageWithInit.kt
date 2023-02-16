package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import software.amazon.awssdk.awscore.presigner.PresignedRequest
import software.amazon.awssdk.http.SdkHttpMethod
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * S3 implementation of Storage with init method
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param prefix a common prefix for all S3 keys in this storage
 * @param K type of key
 */
abstract class AbstractS3StorageWithInit<K>(
    private val s3Operations: S3Operations,
    prefix: String,
) : StorageWrapperWithInit<K>() {
    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
     */
    protected val prefix: String = prefix.removeSuffix(PATH_DELIMITER) + PATH_DELIMITER

    override fun createUnderlyingStorage(): Storage<K> = object : AbstractS3Storage<K>(s3Operations, prefix) {
        override fun buildKey(s3KeySuffix: String): K = this@AbstractS3StorageWithInit.buildKey(s3KeySuffix)
        override fun buildS3KeySuffix(key: K): String = this@AbstractS3StorageWithInit.buildS3KeySuffix(key)
    }

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    protected abstract fun buildKey(s3KeySuffix: String): K

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    protected abstract fun buildS3KeySuffix(key: K): String
}
