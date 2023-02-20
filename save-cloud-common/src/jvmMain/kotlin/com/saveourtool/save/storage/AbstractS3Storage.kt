package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import software.amazon.awssdk.core.async.AsyncRequestBody

import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.CompletableFuture

import kotlin.time.Duration.Companion.minutes

/**
 * S3 implementation of Storage
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param prefix a common prefix for all S3 keys in this storage
 * @param K type of key
 */
abstract class AbstractS3Storage<K>(
    private val s3Operations: S3Operations,
    prefix: String,
) : Storage<K> {
    private val log: Logger = getLogger(this::class)

    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
     */
    protected val prefix: String = prefix.removeSuffix(PATH_DELIMITER) + PATH_DELIMITER

    override fun list(): Flux<K> = s3Operations.listObjectsV2(prefix)
        .toMonoAndPublishOn()
        .expand { lastResponse ->
            if (lastResponse.isTruncated) {
                s3Operations.listObjectsV2(prefix, lastResponse.nextContinuationToken())
                    .toMonoAndPublishOn()
            } else {
                Mono.empty()
            }
        }
        .flatMapIterable { response ->
            response.contents().map {
                buildKey(it.key())
            }
        }

    override fun download(key: K): Flux<ByteBuffer> = s3Operations.getObject(buildS3Key(key))
        .toMonoAndPublishOn()
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

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> =
            s3Operations.createMultipartUpload(buildS3Key(key))
                .toMonoAndPublishOn()
                .flatMap { response ->
                    content.index()
                        .flatMap { (index, buffer) ->
                            s3Operations.uploadPart(response, index + 1, AsyncRequestBody.fromByteBuffer(buffer))
                                .toMonoAndPublishOn()
                        }
                        .collectList()
                        .flatMap { uploadPartResults ->
                            s3Operations.completeMultipartUpload(response, uploadPartResults)
                                .toMonoAndPublishOn()
                        }
                }
                .flatMap {
                    contentLength(key)
                }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> =
            s3Operations.putObject(buildS3Key(key), contentLength, AsyncRequestBody.fromPublisher(content))
                .toMonoAndPublishOn()
                .map { response ->
                    log.debug { "Uploaded $key with versionId: ${response.versionId()}" }
                }

    override fun move(source: K, target: K): Mono<Boolean> =
            s3Operations.copyObject(buildS3Key(source), buildS3Key(target))
                .toMonoAndPublishOn()
                .flatMap {
                    delete(source)
                }

    override fun delete(key: K): Mono<Boolean> = s3Operations.deleteObject(buildS3Key(key))
        .toMonoAndPublishOn()
        .thenReturn(true)
        .defaultIfEmpty(false)

    override fun lastModified(key: K): Mono<Instant> = s3Operations.headObject(buildS3Key(key))
        .toMonoAndPublishOn()
        .map { response ->
            response.lastModified()
        }

    override fun contentLength(key: K): Mono<Long> = s3Operations.headObject(buildS3Key(key))
        .toMonoAndPublishOn()
        .map { response ->
            response.contentLength()
        }

    override fun doesExist(key: K): Mono<Boolean> = s3Operations.headObject(buildS3Key(key))
        .toMonoAndPublishOn()
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

    private fun <T : Any> CompletableFuture<out T?>.toMonoAndPublishOn(): Mono<T> = toMono().publishOn(s3Operations.scheduler)

    companion object {
        private val downloadDuration = 15.minutes
        private fun String.validateSuffix(): String = also { suffix ->
            require(!suffix.startsWith(PATH_DELIMITER)) {
                "Suffix cannot start with $PATH_DELIMITER: $suffix"
            }
        }
    }
}
