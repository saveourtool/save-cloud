package com.saveourtool.save.s3

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.ResponsePublisher
import software.amazon.awssdk.services.s3.model.*
import java.nio.ByteBuffer

interface S3Operations {
    fun listObjectsV2(prefix: String): Flux<ListObjectsV2Response>

    fun getObject(s3Key: String): Mono<ResponsePublisher<GetObjectResponse>>

    fun uploadObject(s3Key: String, content: Flux<ByteBuffer>): Mono<CompleteMultipartUploadResponse>

    fun uploadObject(s3key: String, contentLength: Long, content: Flux<ByteBuffer>): Mono<PutObjectResponse>

    fun deleteObject(s3key: String): Mono<DeleteObjectResponse>

    fun headObject(s3key: String): Mono<HeadObjectResponse>

}