package com.saveourtool.save.storage

import com.saveourtool.save.utils.getLogger

import org.slf4j.Logger
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*

import java.nio.ByteBuffer
import java.time.Instant

/**
 * S3 implementation of Storage
 *
 * @param s3Client async S3 client to operate with S3 storage
 * @param bucketName
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @param K type of key
 */
abstract class AbstractS3Storage<K>(
    private val s3Client: S3AsyncClient,
    private val bucketName: String,
    prefix: String,
) : Storage<K> {
    private val log: Logger = getLogger(this::class)
    private val prefix = prefix.removeSuffix(PATH_DELIMITER) + PATH_DELIMITER

    override fun list(): Flux<K> = listObjectsV2()
        .flatMapIterable { response ->
            response.contents().map {
                buildKey(it.key().removePrefix(prefix))
            }
        }

    private fun listObjectsV2(): Flux<ListObjectsV2Response> = doListObjectsV2().expand { lastResponse ->
        if (lastResponse.isTruncated) {
            doListObjectsV2(lastResponse.nextContinuationToken())
        } else {
            Mono.empty()
        }
    }

    private fun doListObjectsV2(continuationToken: String? = null): Mono<ListObjectsV2Response> = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .prefix(prefix)
        .let { builder ->
            continuationToken?.let { builder.continuationToken(it) } ?: builder
        }
        .build()
        .let {
            s3Client.listObjectsV2(it).toMono()
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
                content.index()
                    .flatMap { (index, buffer) ->
                        response.uploadPart(index, buffer)
                    }
                    .collectList()
                    .flatMap { completedParts ->
                        val completeRequest = CompleteMultipartUploadRequest.builder()
                            .bucket(response.bucket())
                            .key(response.key())
                            .uploadId(response.uploadId())
                            .multipartUpload { builder ->
                                builder.parts(completedParts)
                            }
                            .build()
                        s3Client.completeMultipartUpload(completeRequest)
                            .toMono()
                    }
            }
            .flatMap {
                contentSize(key)
            }
    }

    private fun CreateMultipartUploadResponse.uploadPart(index: Long, contentPart: ByteBuffer): Mono<CompletedPart> {
        val nextPartRequest = UploadPartRequest.builder()
            .bucket(bucket())
            .key(key())
            .uploadId(uploadId())
            .partNumber(index.toInt())
            .build()
        val nextPartRequestBody = AsyncRequestBody.fromByteBuffer(contentPart)
        return s3Client.uploadPart(nextPartRequest, nextPartRequestBody)
            .toMono()
            .map { partResponse ->
                CompletedPart.builder()
                    .eTag(partResponse.eTag())
                    .partNumber(index.toInt())
                    .build()
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
        private fun String.validateSuffix(): String = also { suffix ->
            require(!suffix.startsWith(PATH_DELIMITER)) {
                "Suffix cannot start with $PATH_DELIMITER: $suffix"
            }
        }
    }
}
