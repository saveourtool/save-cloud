package com.saveourtool.save.storage

import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.warn
import org.slf4j.Logger
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.nio.ByteBuffer
import java.time.Instant
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import software.amazon.awssdk.core.async.AsyncRequestBody

abstract class AbstractS3Storage<K>(
    private val s3Client: S3AsyncClient,
    private val bucketName: String,
    prefix: String,
): Storage<K> {
    private val log: Logger = getLogger(this::class)
    private val prefixWithPathDelimiterEnding = prefix.removeSuffix(PATH_DELIMITER) + PATH_DELIMITER

    override fun list(): Flux<K> = listObjectsV2()
        .flatMapIterable { response ->
            response.contents().map {
                buildKey(it.key().removePrefix(prefixWithPathDelimiterEnding))
            }
        }

    private fun listObjectsV2(): Flux<ListObjectsV2Response> = doListObjectsV2().expand { lastResponse ->
        if (lastResponse.isTruncated) {
            doListObjectsV2(lastResponse.nextContinuationToken())
        } else {
            Mono.empty()
        }
    }

    private fun doListObjectsV2(continuationToken: String? = null): Mono<ListObjectsV2Response> {
        return ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefixWithPathDelimiterEnding)
            .let { builder ->
                continuationToken?.let { builder.continuationToken(it) } ?: builder
            }
            .build()
            .let {
                s3Client.listObjectsV2(it).toMono()
            }
    }

    override fun download(key: K): Flux<ByteBuffer> {
        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(buildS3Key(key))
            .build()

        return s3Client.getObject(request, AsyncResponseTransformer.toPublisher())
            .toMono()
            .flatMapMany { response ->
                Flux.from(response)
            }
    }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> {
        val request = CreateMultipartUploadRequest.builder()
            .bucket(bucketName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .key(buildS3Key(key))
            .build()
        return s3Client.createMultipartUpload(request)
            .toMono()
            .flatMap { response ->
                content.index().flatMap { (index, buffer) ->
                    val nextPartRequest = UploadPartRequest.builder()
                        .bucket(response.bucket())
                        .key(response.key())
                        .uploadId(response.uploadId())
                        .partNumber(index.toInt())
//                        .contentLength(buffer.capacity().toLong())
                        .build()
                    s3Client.uploadPart(nextPartRequest, AsyncRequestBody.fromByteBuffer(buffer))
                        .toMono()
                        .map { partResponse ->
                            CompletedPart.builder()
                                .eTag(partResponse.eTag())
                                .partNumber(index.toInt())
                                .build()
                        }
                }
                    .collectList()
                    .flatMap { completedParts ->
                        val completeRequest = CompleteMultipartUploadRequest.builder()
                            .bucket(response.bucket())
                            .key(response.key())
                            .uploadId(response.uploadId())
                            .multipartUpload {
                                it.parts(completedParts)
                            }
                            .build()
                        s3Client.completeMultipartUpload(completeRequest)
                            .toMono()
                    }
            }
            .flatMap { response ->
                s3Client.headObject {
                    it.bucket(response.bucket()).key(response.key())
                }.toMono().map { it.contentLength() }
            }
    }

    override fun delete(key: K): Mono<Boolean> {
        val request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(buildS3Key(key))
            .build()
        return s3Client.deleteObject(request)
            .toMono()
            .thenReturn(true)
            .onErrorResume { ex ->
                log.warn(ex) {
                    "Failed to delete key $key"
                }
                false.toMono()
            }
    }

    override fun lastModified(key: K): Mono<Instant> = headObjectAsMono(key)
        .map { response ->
            response.lastModified()
        }

    override fun contentSize(key: K): Mono<Long> = headObjectAsMono(key)
        .map { response ->
            response.contentLength()
        }

    override fun doesExist(key: K): Mono<Boolean> = headObjectAsMono(key)
        .map { true }
        .defaultIfEmpty(false)

    private fun headObjectAsMono(key: K) = HeadObjectRequest.builder()
        .bucket(bucketName)
        .key(buildS3Key(key))
        .build()
        .let { s3Client.headObject(it) }
        .toMono()

    protected abstract fun buildKey(s3KeySuffix: String): K

    protected abstract fun buildS3KeySuffix(key: K): String

    private fun buildS3Key(key: K) = prefixWithPathDelimiterEnding + buildS3KeySuffix(key)

    companion object {
        const val PATH_DELIMITER = "/"
    }
}