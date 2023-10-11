package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.AbstractS3KeyDatabaseManager
import com.saveourtool.save.storage.key.S3KeyManager
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.CompletedPart

import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * S3 implementation of [StorageProjectReactor]
 *
 * @param K type of key
 * @property s3Operations [S3Operations] to operate with S3
 * @property s3KeyManager [S3KeyManager] manager for S3 keys
 */
class DefaultStorageProjectReactor<K : Any>(
    private val s3Operations: S3Operations,
    private val s3KeyManager: S3KeyManager<K>,
) : StorageProjectReactor<K> {
    private val log: Logger = getLogger(this::class)

    override fun list(): Flux<K> = s3Operations.listObjectsV2(s3KeyManager.commonPrefix)
        .toMonoAndPublishOn()
        .expand { lastResponse ->
            if (lastResponse.isTruncated) {
                s3Operations.listObjectsV2(s3KeyManager.commonPrefix, lastResponse.nextContinuationToken())
                    .toMonoAndPublishOn()
            } else {
                Mono.empty()
            }
        }
        .flatMapIterable { response ->
            response.contents().map { it.key() }
        }
        .flatMap { s3Key ->
            findKey(s3Key)
                .switchIfEmpty {
                    log.warn {
                        "Found s3 key $s3Key which is not valid by ${s3KeyManager::class.simpleName}"
                    }
                    Mono.empty()
                }
        }

    override fun download(key: K): Flux<ByteBuffer> = findExistedS3Key(key)
        .flatMap { s3Key ->
            s3Operations.getObject(s3Key)
                .toMonoAndPublishOn()
        }
        .flatMapMany {
            it.toFlux()
        }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<K> =
            createNewS3Key(key)
                .flatMap { s3Key ->
                    s3Operations.createMultipartUpload(s3Key)
                        .toMonoAndPublishOn()
                        .flatMap { response ->
                            content.bufferAccumulatedUntil { buffers ->
                                buffers.sumOf { it.capacity().toLong() } < multiPartUploadMinPartSizeInBytes
                            }
                                .index()
                                .flatMap { (index, buffers) ->
                                    val contentLength = buffers.sumOf { it.capacity().toLong() }
                                    s3Operations.uploadPart(response, index + 1, AsyncRequestBody.fromByteBuffers(*buffers.toTypedArray()))
                                        .toMonoAndPublishOn()
                                        .map { it to contentLength }
                                }
                                .collectList()
                                .flatMap { uploadPartResultWithSizeList ->
                                    s3Operations.completeMultipartUpload(response, uploadPartResultWithSizeList.map { it.completedPart() })
                                        .toMonoAndPublishOn()
                                        .map {
                                            uploadPartResultWithSizeList.sumOf { it.contentLength() }
                                        }
                                }
                        }
                        .flatMap { contentLength ->
                            findKeyAndUpdateByContentLength(s3Key, contentLength)
                                .switchIfEmptyToNotFound {
                                    "Not found inserted updated key for $key"
                                }
                        }
                }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> =
            createNewS3Key(key)
                .flatMap { s3Key ->
                    s3Operations.putObject(s3Key, contentLength, AsyncRequestBody.fromPublisher(content))
                        .toMonoAndPublishOn()
                        .doOnError {
                            s3KeyManager.delete(key)
                        }
                        .flatMap {
                            findKeyAndUpdateByContentLength(s3Key, contentLength)
                                .switchIfEmptyToNotFound {
                                    "Not found inserted updated key for $key"
                                }
                        }
                }

    override fun move(source: K, target: K): Mono<Boolean> =
            findExistedS3Key(source)
                .zipWith(createNewS3Key(target))
                .flatMap { (sourceS3Key, targetS3Key) ->
                    s3Operations.copyObject(sourceS3Key, targetS3Key)
                        .toMonoAndPublishOn()
                }
                .flatMap {
                    delete(source)
                }

    override fun delete(key: K): Mono<Boolean> = findExistedS3Key(key).flatMap { s3Key ->
        s3Operations.deleteObject(s3Key)
            .toMonoAndPublishOn()
    }
        .flatMap { deleteKey(key) }
        .thenReturn(true)
        .defaultIfEmpty(false)

    override fun deleteAll(keys: Collection<K>): Mono<Boolean> = keys.toFlux()
        .flatMap { findExistedS3Key(it) }
        .flatMap { s3Key ->
            s3Operations.deleteObject(s3Key).toMonoAndPublishOn()
        }
        .flatMap { deleteKeys(keys) }
        .thenJust(true)
        .defaultIfEmpty(false)

    override fun lastModified(key: K): Mono<Instant> = findExistedS3Key(key).flatMap { s3Key ->
        s3Operations.headObject(s3Key)
            .toMonoAndPublishOn()
    }
        .map { response ->
            response.lastModified()
        }

    override fun contentLength(key: K): Mono<Long> = findExistedS3Key(key).flatMap { s3Key ->
        s3Operations.headObject(s3Key)
            .toMonoAndPublishOn()
    }
        .map { response ->
            response.contentLength()
        }

    override fun doesExist(key: K): Mono<Boolean> = findExistedS3Key(key).flatMap { s3Key ->
        s3Operations.headObject(s3Key)
            .toMonoAndPublishOn()
    }
        .map { true }
        .defaultIfEmpty(false)

    private fun <T : Any> CompletableFuture<out T?>.toMonoAndPublishOn(): Mono<T> = toMono().publishOn(s3Operations.scheduler)

    private fun deleteKey(key: K): Mono<Unit> = s3KeyManager.callAsMono { delete(key) }

    private fun deleteKeys(keys: Collection<K>): Mono<Unit> = s3KeyManager.callAsMono { deleteAll(keys) }

    private fun findKey(s3Key: String): Mono<K> = s3KeyManager.callAsMono { findKey(s3Key) }

    private fun findKeyAndUpdateByContentLength(s3Key: String, contentLength: Long): Mono<K> = s3KeyManager.callAsMono {
        findKey(s3Key)?.let {
            updateKeyByContentLength(it,
                contentLength)
        }
    }

    private fun findExistedS3Key(key: K): Mono<String> = s3KeyManager.callAsMono { findExistedS3Key(key) }

    private fun createNewS3Key(key: K): Mono<String> = s3KeyManager.callAsMono { createNewS3Key(key) }

    private fun <R : Any> S3KeyManager<K>.callAsMono(function: S3KeyManager<K>.() -> R?): Mono<R> =
            if (s3KeyManager is AbstractS3KeyDatabaseManager<*, *, *>) {
                s3KeyManager.blockingBridge.blockingToMono { function(this) }
            } else {
                { function(this) }.toMono()
            }

    private fun Pair<CompletedPart, Long>.completedPart() = first

    private fun Pair<CompletedPart, Long>.contentLength() = second

    companion object {
        private val multiPartUploadMinPartSizeInBytes = 5 * 1024 * 1024
    }
}
