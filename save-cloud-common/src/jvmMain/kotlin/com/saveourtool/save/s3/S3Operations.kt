package com.saveourtool.save.s3

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.BoundedElasticScheduler
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.core.async.ResponsePublisher
import software.amazon.awssdk.services.s3.model.*
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import reactor.core.scheduler.BoundedElasticScheduler
import reactor.core.scheduler.Schedulers

interface S3Operations {

    private val executor = ThreadPoolExecutor(
        Schedulers.DEFAULT_POOL_SIZE,
        Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
        BoundedElasticScheduler.DEFAULT_TTL_SECONDS,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE)
    )

    private val test = Executors.newFixedThreadPool(4)
    fun listObjectsV2(prefix: String): Flux<ListObjectsV2Response>

    fun getObject(s3Key: String): Mono<ResponsePublisher<GetObjectResponse>>

    fun uploadObject(s3Key: String, content: Flux<ByteBuffer>): Mono<CompleteMultipartUploadResponse>

    fun uploadObject(s3key: String, contentLength: Long, content: Flux<ByteBuffer>): Mono<PutObjectResponse>

    fun deleteObject(s3key: String): Mono<DeleteObjectResponse>

    fun headObject(s3key: String): Mono<HeadObjectResponse>

}