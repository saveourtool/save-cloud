package com.saveourtool.save.s3

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.ResponsePublisher
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import java.nio.ByteBuffer
import kotlin.time.Duration

typealias GetObjectResponsePublisher = ResponsePublisher<GetObjectResponse>

/**
 * Base interface to operate with S3 in single bucket (like JdbcOperations)
 */
interface S3Operations {
    /**
     * @param prefix
     * @return [Flux] with all response
     */
    fun listObjectsV2(prefix: String): Flux<ListObjectsV2Response>

    /**
     * @param s3Key
     * @return [Mono] with response as [ResponsePublisher]
     */
    fun getObject(s3Key: String): Mono<GetObjectResponsePublisher>

    /**
     * @param s3Key
     * @param content
     * @return [Mono] with response
     */
    fun uploadObject(s3Key: String, content: Flux<ByteBuffer>): Mono<CompleteMultipartUploadResponse>

    /**
     * @param s3Key
     * @param contentLength
     * @param content
     * @return [Mono] with response
     */
    fun uploadObject(s3Key: String, contentLength: Long, content: Flux<ByteBuffer>): Mono<PutObjectResponse>

    /**
     * @param sourceS3Key
     * @param targetS3Key
     * @return [Mono] with response
     */
    fun copyObject(sourceS3Key: String, targetS3Key: String): Mono<CopyObjectResponse>

    /**
     * @param s3Key
     * @return [Mono] with response
     */
    fun deleteObject(s3Key: String): Mono<DeleteObjectResponse>

    /**
     * @param s3Key
     * @return [Mono] with response
     */
    fun headObject(s3Key: String): Mono<HeadObjectResponse>

    /**
     * @param s3Key
     * @param duration duration when url is valid
     * @return a pre-signed request to download an object
     */
    fun requestToDownloadObject(s3Key: String, duration: Duration): PresignedGetObjectRequest

    /**
     * @param s3Key
     * @param contentLength
     * @param duration duration when url is valid
     * @return a pre-signed request to upload an object
     */
    fun requestToUploadObject(s3Key: String, contentLength: Long, duration: Duration): PresignedPutObjectRequest
}
