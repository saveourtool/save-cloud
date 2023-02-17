package com.saveourtool.save.s3

import org.springframework.http.MediaType
import reactor.core.scheduler.Schedulers
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

import java.net.URI
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.asCoroutineDispatcher

/**
 * Default implementation of [S3Operations]
 *
 * @param properties S3 properties
 */
class DefaultS3Operations(
    properties: S3OperationsProperties,
) : S3Operations, AutoCloseable {
    private val bucketName = properties.bucketName
    private val credentialsProvider: AwsCredentialsProvider = with(properties) {
        StaticCredentialsProvider.create(
            credentials.toAwsCredentials()
        )
    }
    private val executorName: String = "s3-operations-${properties.bucketName}"
    override val executorService = with(properties.async) {
        ThreadPoolExecutor(
            minPoolSize,
            maxPoolSize,
            ttl.toNanos(),
            TimeUnit.NANOSECONDS,
            LinkedBlockingQueue(queueSize),
            NamedDefaultThreadFactory(executorName),
        )
    }
    override val scheduler = Schedulers.fromExecutorService(executorService, executorName)
    override val coroutineDispatcher: CoroutineDispatcher = scheduler.asCoroutineDispatcher()
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

    override fun listObjectsV2(prefix: String, continuationToken: String?): CompletableFuture<ListObjectsV2Response> {
        val request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .let { builder ->
                continuationToken?.let { builder.continuationToken(it) } ?: builder
            }
            .build()
        return s3Client.listObjectsV2(request)
    }

    private fun getObjectRequest(s3Key: String) = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(s3Key)
        .build()

    override fun getObject(s3Key: String): CompletableFuture<GetObjectResponsePublisher?> =
            s3Client.getObject(getObjectRequest(s3Key), AsyncResponseTransformer.toPublisher())
                .handleNoSuchKeyException()

    override fun createMultipartUpload(s3Key: String): CompletableFuture<CreateMultipartUploadResponse> {
        val request = CreateMultipartUploadRequest.builder()
            .bucket(bucketName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .key(s3Key)
            .build()
        return s3Client.createMultipartUpload(request)
    }

    override fun uploadPart(createResponse: CreateMultipartUploadResponse, index: Long, contentPart: AsyncRequestBody): CompletableFuture<CompletedPart> {
        val nextPartRequest = UploadPartRequest.builder()
            .bucket(createResponse.bucket())
            .key(createResponse.key())
            .uploadId(createResponse.uploadId())
            .partNumber(index.toInt())
            .build()
        return s3Client.uploadPart(nextPartRequest, contentPart)
            .thenApply { partResponse ->
                CompletedPart.builder()
                    .eTag(partResponse.eTag())
                    .partNumber(index.toInt())
                    .build()
            }
    }

    override fun completeMultipartUpload(
        createResponse: CreateMultipartUploadResponse,
        completedParts: Collection<CompletedPart>
    ): CompletableFuture<CompleteMultipartUploadResponse> {
        val request = CompleteMultipartUploadRequest.builder()
            .bucket(createResponse.bucket())
            .key(createResponse.key())
            .uploadId(createResponse.uploadId())
            .multipartUpload { builder ->
                builder.parts(completedParts.sortedBy { it.partNumber() })
            }
            .build()
        return s3Client.completeMultipartUpload(request)
    }

    private fun putObjectRequest(s3Key: String, contentLength: Long): PutObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .key(s3Key)
        .contentLength(contentLength)
        .build()

    override fun putObject(s3Key: String, contentLength: Long, content: AsyncRequestBody): CompletableFuture<PutObjectResponse> =
            s3Client.putObject(putObjectRequest(s3Key, contentLength), content)

    override fun copyObject(sourceS3Key: String, targetS3Key: String): CompletableFuture<CopyObjectResponse?> {
        val request = CopyObjectRequest.builder()
            .sourceBucket(bucketName)
            .sourceKey(sourceS3Key)
            .destinationBucket(bucketName)
            .destinationKey(targetS3Key)
            .build()
        return s3Client.copyObject(request)
            .handleNoSuchKeyException()
    }

    override fun deleteObject(s3Key: String): CompletableFuture<DeleteObjectResponse?> {
        val request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build()
        return s3Client.deleteObject(request)
            .handleNoSuchKeyException()
    }

    override fun headObject(s3Key: String): CompletableFuture<HeadObjectResponse?> {
        val request = HeadObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build()
        return s3Client.headObject(request)
            .handleNoSuchKeyException()
    }

    override fun requestToDownloadObject(
        s3Key: String,
        duration: Duration,
    ): PresignedGetObjectRequest = s3Presigner.presignGetObject { builder ->
        builder
            .signatureDuration(duration.toJavaDuration())
            .getObjectRequest(getObjectRequest(s3Key))
            .build()
    }

    companion object {
        private val region = Region.AWS_ISO_GLOBAL

        private fun <T : Any> CompletableFuture<T>.handleNoSuchKeyException(): CompletableFuture<T?> = exceptionally { ex ->
            when (ex) {
                is NoSuchKeyException -> null
                else -> throw ex
            }
        }

        private fun URI.lowercase(): URI = URI(toString().lowercase())

        private class NamedDefaultThreadFactory(private val namePrefix: String) : ThreadFactory {
            private val delegate: ThreadFactory = Executors.defaultThreadFactory()
            private val threadCount = AtomicInteger(0)

            override fun newThread(runnable: Runnable): Thread {
                val thread = delegate.newThread(runnable).apply {
                    name = namePrefix + "-" + threadCount.getAndIncrement()
                }
                return thread
            }
        }
    }
}
