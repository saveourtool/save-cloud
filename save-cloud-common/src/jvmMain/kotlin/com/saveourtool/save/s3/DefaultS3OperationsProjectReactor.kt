package com.saveourtool.save.s3

import io.ktor.utils.io.*
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
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
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest

import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.*

import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactor.asCoroutineDispatcher
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext

/**
 * Default implementation of [S3Operations]
 *
 * @param properties S3 properties
 */
class DefaultS3OperationsProjectReactor(
    properties: S3OperationsProperties,
) : S3OperationsProjectReactor, AutoCloseable {
    private val bucketName = properties.bucketName
    private val credentialsProvider: AwsCredentialsProvider = properties.credentials.toAwsCredentialsProvider()
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
    private val coroutineDispatcher = scheduler.asCoroutineDispatcher()
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
            .region(region)
            .forcePathStyle(true)
            .endpointOverride(endpoint)
            .build()
            .also { createdClient ->
                if (createBucketIfNotExists) {
                    createdClient.createBucketIfNotExists(bucketName).join()
                }
            }
    }
    private val s3Presigner: S3Presigner = with(properties) {
        S3Presigner.builder()
            .credentialsProvider(credentialsProvider)
            .region(region)
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .endpointOverride(presignedEndpoint.lowercase())
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
            .thenApply { partResponse ->
                CompletedPart.builder()
                    .eTag(partResponse.eTag())
                    .partNumber(index.toInt())
                    .build()
            }
            .toMonoAndPublishOn()
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

    override suspend fun uploadObject(s3Key: String, contentLength: Long, content: ByteReadChannel): PutObjectResponse {
        val contentAsFlux = flow {
            content.consumeEachBufferRange { buffer, last ->
                emit(buffer)
                !last
            }
        }
            .asFlux(coroutineDispatcher)
        return s3Client.putObject(putObjectRequest(s3Key, contentLength), AsyncRequestBody.fromPublisher(contentAsFlux))
            .toMonoAndPublishOn()
            .awaitSingleOrNull()
            ?: throw IllegalStateException("Failed to upload $s3Key")
    }

    override suspend fun uploadObject(s3Key: String, contentLength: Long, content: Flow<ByteBuffer>): PutObjectResponse = withContext(coroutineDispatcher) {
        s3Client.putObject(
            putObjectRequest(s3Key, contentLength),
            AsyncRequestBody.fromPublisher(content.flowOn(coroutineDispatcher).asPublisher(coroutineDispatcher))
        )
            .asDeferred()
            .await()
    }

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

    override fun requestToUploadObject(
        s3Key: String,
        contentLength: Long,
        duration: Duration,
    ): PresignedPutObjectRequest = s3Presigner.presignPutObject { builder ->
        builder
            .signatureDuration(duration.toJavaDuration())
            .putObjectRequest(putObjectRequest(s3Key, contentLength))
            .build()
    }

    private fun <T : Any> CompletableFuture<T>.toMonoAndPublishOn(): Mono<T> = toMono().publishOn(scheduler)

    companion object {
        private val region = Region.AWS_ISO_GLOBAL

        private fun <T : Any> Mono<T>.handleNoSuchKeyException(): Mono<T> = onErrorResume(NoSuchKeyException::class.java) {
            Mono.empty()
        }

        private fun URI.lowercase(): URI = URI(toString().lowercase())

        private fun S3AsyncClient.createBucketIfNotExists(bucketName: String): CompletableFuture<String> =
                HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build()
                    .let { headBucket(it) }
                    .thenApply { true }
                    .exceptionally { ex ->
                        when (ex.cause) {
                            is NoSuchBucketException -> false
                            else -> throw ex
                        }
                    }
                    .thenCompose { bucketExists ->
                        if (bucketExists) {
                            CompletableFuture.completedFuture("Bucket $bucketName already exists")
                        } else {
                            CreateBucketRequest.builder()
                                .bucket(bucketName)
                                .build()
                                .let { createBucket(it) }
                                .thenApply { _ ->
                                    "Created bucket $bucketName"
                                }
                        }
                    }
    }
}
