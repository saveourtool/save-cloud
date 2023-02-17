package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.S3KeyAdapter

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

import java.nio.ByteBuffer
import java.time.Instant

import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import software.amazon.awssdk.core.async.AsyncRequestBody
import java.util.concurrent.CompletableFuture

/**
 * S3 implementation of [StorageProjectReactor]
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param s3KeyAdapter [S3KeyAdapter] adapter for S3 keys
 * @param K type of key
 */
class DefaultStorageProjectReactor<K : Any>(
    private val s3Operations: S3Operations,
    private val s3KeyAdapter: S3KeyAdapter<K>,
) : StorageProjectReactor<K> {
    override fun list(): Flux<K> = s3Operations.listObjectsV2(s3KeyAdapter.commonPrefix)
        .toMonoAndPublishOn()
        .expand { lastResponse ->
            if (lastResponse.isTruncated) {
                s3Operations.listObjectsV2(s3KeyAdapter.commonPrefix, lastResponse.nextContinuationToken())
                    .toMonoAndPublishOn()
            } else {
                Mono.empty()
            }
        }
        .flatMapIterable { response ->
            response.contents().map {
                s3KeyAdapter.buildKey(it.key())
            }
        }

    override fun doesExist(key: K): Mono<Boolean> = s3Operations.headObject(s3KeyAdapter.buildS3Key(key))
        .toMonoAndPublishOn()
        .map { true }
        .defaultIfEmpty(false)

    override fun contentLength(key: K): Mono<Long> = s3Operations.headObject(s3KeyAdapter.buildS3Key(key))
        .toMonoAndPublishOn()
        .map { response ->
            response.contentLength()
        }

    override fun lastModified(key: K): Mono<Instant> = s3Operations.headObject(s3KeyAdapter.buildS3Key(key))
        .toMonoAndPublishOn()
        .map { response ->
            response.lastModified()
        }

    override fun delete(key: K): Mono<Boolean> = s3Operations.deleteObject(s3KeyAdapter.buildS3Key(key))
        .toMonoAndPublishOn()
        .thenReturn(true)
        .defaultIfEmpty(false)

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> =
            s3Operations.createMultipartUpload(s3KeyAdapter.buildS3Key(key))
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
            s3Operations.putObject(s3KeyAdapter.buildS3Key(key), contentLength, AsyncRequestBody.fromPublisher(content))
                .toMonoAndPublishOn()
                .thenReturn(Unit)

    override fun download(key: K): Flux<ByteBuffer> = s3Operations.getObject(s3KeyAdapter.buildS3Key(key))
        .toMonoAndPublishOn()
        .flatMapMany {
            it.toFlux()
        }

    override fun move(source: K, target: K): Mono<Boolean> =
            s3Operations.copyObject(s3KeyAdapter.buildS3Key(source), s3KeyAdapter.buildS3Key(target))
                .toMonoAndPublishOn()
                .flatMap {
                    delete(source)
                }


    private fun <T : Any> CompletableFuture<out T?>.toMonoAndPublishOn(): Mono<T> = toMono().publishOn(s3Operations.scheduler)
}
