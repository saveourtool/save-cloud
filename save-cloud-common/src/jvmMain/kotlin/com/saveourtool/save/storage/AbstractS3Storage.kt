package com.saveourtool.save.storage

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CommonPrefix
import java.nio.ByteBuffer
import java.time.Instant

abstract class AbstractS3Storage<K>(
    private val s3Client: S3AsyncClient,
    private val bucketName: String,
    private val commonPrefix: String,
): Storage<K> {
    override fun list(): Flux<K> {
        s3Client.listObjectsV2 { builder ->
            builder.bucket(bucketName)
                .prefix(commonPrefix)
        }
            .contents()
        TODO("Not yet implemented")
    }

    override fun download(key: K): Flux<ByteBuffer> {
        TODO("Not yet implemented")
    }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> {
        TODO("Not yet implemented")
    }

    override fun delete(key: K): Mono<Boolean> {
        TODO("Not yet implemented")
    }

    override fun lastModified(key: K): Mono<Instant> {
        TODO("Not yet implemented")
    }

    override fun contentSize(key: K): Mono<Long> {
        TODO("Not yet implemented")
    }

    override fun doesExist(key: K): Mono<Boolean> {
        TODO("Not yet implemented")
    }
}