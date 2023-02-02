package com.saveourtool.save.s3

import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.BoundedElasticScheduler
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.core.async.ResponsePublisher
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.nio.ByteBuffer
import java.util.concurrent.*

/**
 * Default implementation of [S3Operations]
 *
 * @param s3Client async S3 client to operate with S3
 * @param bucketName a bucket name for all keys in this [S3Operations]
 */
class DefaultS3Operations(
    private val s3Client: S3AsyncClient,
    private val bucketName: String,
) : S3Operations {

    private val executor = ThreadPoolExecutor(
        Schedulers.DEFAULT_POOL_SIZE,
        Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
        BoundedElasticScheduler.DEFAULT_TTL_SECONDS,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE)
    )

    private val test = Executors.newFixedThreadPool(4)

    override fun listObjectsV2(prefix: String): Flux<ListObjectsV2Response> = doListObjectsV2(prefix).expand { lastResponse ->
        if (lastResponse.isTruncated) {
            doListObjectsV2(prefix, lastResponse.nextContinuationToken())
        } else {
            Mono.empty()
        }
    }

    private fun doListObjectsV2(prefix: String, continuationToken: String? = null): Mono<ListObjectsV2Response> = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .prefix(prefix)
        .let { builder ->
            continuationToken?.let { builder.continuationToken(it) } ?: builder
        }
        .build()
        .let {
            s3Client.listObjectsV2(it).toMonoAndPublishOn()
        }

    override fun getObject(s3Key: String): Mono<ResponsePublisher<GetObjectResponse>> {
        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build()

        return s3Client.getObject(request, AsyncResponseTransformer.toPublisher())
            .toMonoAndPublishOn()
            .handleNoSuchKeyException()
    }

    override fun uploadObject(s3Key: String, content: Flux<ByteBuffer>): Mono<CompleteMultipartUploadResponse> {
        val request = CreateMultipartUploadRequest.builder()
            .bucket(bucketName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .key(s3Key)
            .build()
        return s3Client.createMultipartUpload(request)
            .toMonoAndPublishOn()
            .flatMap { response ->
                content.index()
                    .flatMap { (index, buffer) ->
                        response.uploadPart(index + 1, buffer)
                    }
                    .collectList()
                    .flatMap { uploadPartResults ->
                        val completeRequest = CompleteMultipartUploadRequest.builder()
                            .bucket(response.bucket())
                            .key(response.key())
                            .uploadId(response.uploadId())
                            .multipartUpload { builder ->
                                builder.parts(uploadPartResults.sortedBy { it.partNumber() })
                            }
                            .build()
                        s3Client.completeMultipartUpload(completeRequest)
                            .toMonoAndPublishOn()
                    }
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
            .toMonoAndPublishOn()
            .map { partResponse ->
                CompletedPart.builder()
                    .eTag(partResponse.eTag())
                    .partNumber(index.toInt())
                    .build()
            }
    }

    override fun uploadObject(s3key: String, contentLength: Long, content: Flux<ByteBuffer>): Mono<PutObjectResponse> {
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .key(s3key)
            .contentLength(contentLength)
            .build()
        return s3Client.putObject(request, AsyncRequestBody.fromPublisher(content))
            .toMonoAndPublishOn()
    }

    override fun deleteObject(s3key: String): Mono<DeleteObjectResponse> {
        val request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(s3key)
            .build()
        return s3Client.deleteObject(request)
            .toMonoAndPublishOn()
            .handleNoSuchKeyException()
    }

    override fun headObject(s3key: String): Mono<HeadObjectResponse> = HeadObjectRequest.builder()
        .bucket(bucketName)
        .key(s3key)
        .build()
        .let { s3Client.headObject(it) }
        .toMonoAndPublishOn()
        .handleNoSuchKeyException()

    companion object {
        private val scheduler = Schedulers.newBoundedElastic(5, 1000, "s3-storage")

        private fun <T : Any> Mono<T>.handleNoSuchKeyException(): Mono<T> = onErrorResume(NoSuchKeyException::class.java) {
            Mono.empty()
        }

        private fun <T : Any> CompletableFuture<T>.toMonoAndPublishOn(): Mono<T> = toMono().publishOn(scheduler)
    }
}