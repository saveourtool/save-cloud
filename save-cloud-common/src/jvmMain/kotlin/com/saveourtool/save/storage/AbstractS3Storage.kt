package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * S3 implementation of Storage
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param prefix a common prefix for all S3 keys in this storage
 * @param K type of key
 */
abstract class AbstractS3Storage<K : Any>(
    private val s3Operations: S3Operations,
    prefix: String,
) : Storage<K> {
    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
     */
    protected val prefix: String = prefix.removeSuffix(PATH_DELIMITER) + PATH_DELIMITER

    override fun list(): Flux<K> = s3Operations.listObjectsV2(prefix)
        .flatMapIterable { response ->
            response.contents().map {
                buildKey(it.key().removePrefix(prefix))
            }
        }

    override fun download(key: K): Flux<ByteBuffer> = s3Operations.getObject(buildS3Key(key))
        .flatMapMany {
            it.toFlux()
        }

    override fun generateUrlToDownload(key: K): URL = s3Operations.requestToDownloadObject(buildS3Key(key), downloadDuration)
        .also { request ->
            require(request.isBrowserExecutable) {
                "Pre-singer url to download object should be browser executable (header-less)"
            }
        }
        .url()

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<K> =
            s3Operations.uploadObject(buildS3Key(key), content)
                .thenReturn(key)

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> =
            s3Operations.uploadObject(buildS3Key(key), contentLength, content)
                .thenReturn(key)

    override fun move(source: K, target: K): Mono<Boolean> =
            s3Operations.copyObject(buildS3Key(source), buildS3Key(target))
                .flatMap {
                    delete(source)
                }

    override fun delete(key: K): Mono<Boolean> = s3Operations.deleteObject(buildS3Key(key))
        .thenReturn(true)
        .defaultIfEmpty(false)

    override fun lastModified(key: K): Mono<Instant> = s3Operations.headObject(buildS3Key(key))
        .map { response ->
            response.lastModified()
        }

    override fun contentLength(key: K): Mono<Long> = s3Operations.headObject(buildS3Key(key))
        .map { response ->
            response.contentLength()
        }

    override fun doesExist(key: K): Mono<Boolean> = s3Operations.headObject(buildS3Key(key))
        .map { true }
        .defaultIfEmpty(false)

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

    private fun buildS3Key(key: K) = prefix + buildS3KeySuffix(key).validateSuffix()

    companion object {
        private val downloadDuration = 15.minutes
        private fun String.validateSuffix(): String = also { suffix ->
            require(!suffix.startsWith(PATH_DELIMITER)) {
                "Suffix cannot start with $PATH_DELIMITER: $suffix"
            }
        }
    }
}
