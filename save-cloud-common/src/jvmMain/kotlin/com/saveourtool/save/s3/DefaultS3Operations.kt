package com.saveourtool.save.s3

import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Default implementation of [S3Operations]
 *
 * @param properties S3 properties
 */
class DefaultS3Operations(
    properties: S3OperationsProperties,
) : S3Operations, Closeable {
    private val bucketName = properties.bucketName
    private val credentialsProvider: AwsCredentialsProvider = with(properties) {
        StaticCredentialsProvider.create(
            credentials.toAwsCredentials()
        )
    }
    private val executorService = with(properties.async) {
        ThreadPoolExecutor(
            minPoolSize,
            maxPoolSize,
            ttl.toNanos(),
            TimeUnit.NANOSECONDS,
            LinkedBlockingQueue(queueSize),
        )
    }
    private val scheduler = Schedulers.fromExecutorService(executorService, "s3-operations-${properties.bucketName}-")
    private val s3Client: S3AsyncClient = with(properties) {
        S3AsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .httpClientBuilder(
                NettyNioAsyncHttpClient.builder()
                    .maxConcurrency(httpClient.maxConcurrency)
                    .connectionTimeout(httpClient.connectionTimeout)
                    .connectionAcquisitionTimeout(httpClient.connectionAcquisitionTimeout)
            )
            .asyncConfiguration { builder ->
                builder.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executorService)
            }
            .region(Region.AWS_ISO_GLOBAL)
            .forcePathStyle(true)
            .endpointOverride(endpoint)
            .build()
    }
    private val s3Presigner: S3Presigner = with(properties) {
        S3Presigner.builder()
            .credentialsProvider(credentialsProvider)
            .region(Region.AWS_ISO_GLOBAL)
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .endpointOverride(presignedEndpoint)
            .build()
    }

    override fun close() {
        s3Client.close()
        s3Presigner.close()
        executorService.shutdown()
    }

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

    private fun getObjectRequest(s3Key: String) = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(s3Key)
        .build()

    override fun getObject(s3Key: String): Mono<GetObjectResponsePublisher> =
            s3Client.getObject(getObjectRequest(s3Key), AsyncResponseTransformer.toPublisher())
                .toMonoAndPublishOn()
                .handleNoSuchKeyException()

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

    private fun putObjectRequest(s3Key: String, contentLength: Long): PutObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .key(s3Key)
        .contentLength(contentLength)
        .build()

    override fun uploadObject(s3Key: String, contentLength: Long, content: Flux<ByteBuffer>): Mono<PutObjectResponse> =
            s3Client.putObject(putObjectRequest(s3Key, contentLength), AsyncRequestBody.fromPublisher(content))
                .toMonoAndPublishOn()

    override fun copyObject(sourceS3Key: String, targetS3Key: String): Mono<CopyObjectResponse> {
        val request = CopyObjectRequest.builder()
            .sourceBucket(bucketName)
            .sourceKey(sourceS3Key)
            .destinationBucket(bucketName)
            .destinationKey(targetS3Key)
            .build()
        return s3Client.copyObject(request)
            .toMonoAndPublishOn()
            .handleNoSuchKeyException()
    }

    override fun deleteObject(s3Key: String): Mono<DeleteObjectResponse> {
        val request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build()
        return s3Client.deleteObject(request)
            .toMonoAndPublishOn()
            .handleNoSuchKeyException()
    }

    override fun headObject(s3Key: String): Mono<HeadObjectResponse> = HeadObjectRequest.builder()
        .bucket(bucketName)
        .key(s3Key)
        .build()
        .let { s3Client.headObject(it) }
        .toMonoAndPublishOn()
        .handleNoSuchKeyException()

    override fun requestToDownloadObject(
        s3Key: String,
        duration: Duration,
    ): PresignedGetObjectRequest = s3Presigner.presignGetObject { builder ->
        builder
            .signatureDuration(duration.toJavaDuration())
            .getObjectRequest(getObjectRequest(s3Key))
            .build()
    }

    private fun <T : Any> CompletableFuture<T>.toMonoAndPublishOn(): Mono<T> = toMono().publishOn(scheduler)

    companion object {
        private fun <T : Any> Mono<T>.handleNoSuchKeyException(): Mono<T> = onErrorResume(NoSuchKeyException::class.java) {
            Mono.empty()
        }
    }
}
