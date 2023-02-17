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
 * Implementation of [S3Operations] using ProjectReactor
 *
 * @param s3Operations
 */
class S3OperationsProjectReactorBridge(
    private val s3Operations: S3Operations,
) {
    private val scheduler = Schedulers.fromExecutorService(s3Operations.executorService, s3Operations.executorName)

    fun listObjectsV2(prefix: String): Flux<ListObjectsV2Response> = doListObjectsV2(prefix).expand { lastResponse ->
        if (lastResponse.isTruncated) {
            doListObjectsV2(prefix, lastResponse.nextContinuationToken())
        } else {
            Mono.empty()
        }
    }

    private fun doListObjectsV2(prefix: String, continuationToken: String? = null): Mono<ListObjectsV2Response> =
            s3Operations.listObjectsV2(prefix, continuationToken)
                .toMonoAndPublishOn()

    fun getObject(s3Key: String): Mono<GetObjectResponsePublisher> =
            s3Operations.getObject(s3Key).toMonoAndPublishOn()

    fun uploadObject(s3Key: String, content: Flux<ByteBuffer>): Mono<CompleteMultipartUploadResponse> =
            s3Operations.createMultipartUpload(s3Key)
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

    fun uploadObject(s3Key: String, contentLength: Long, content: Flux<ByteBuffer>): Mono<PutObjectResponse> =
            s3Operations.putObject(s3Key, contentLength, AsyncRequestBody.fromPublisher(content))
                .toMonoAndPublishOn()

    fun copyObject(sourceS3Key: String, targetS3Key: String): Mono<CopyObjectResponse> =
            s3Operations.copyObject(sourceS3Key, targetS3Key)
                .toMonoAndPublishOn()

    fun deleteObject(s3Key: String): Mono<DeleteObjectResponse> = s3Operations.deleteObject(s3Key)
        .toMonoAndPublishOn()

    fun headObject(s3Key: String): Mono<HeadObjectResponse> = s3Operations.headObject(s3Key).toMonoAndPublishOn()

    private fun <T : Any> CompletableFuture<out T?>.toMonoAndPublishOn(): Mono<T> = toMono().publishOn(scheduler)
}
