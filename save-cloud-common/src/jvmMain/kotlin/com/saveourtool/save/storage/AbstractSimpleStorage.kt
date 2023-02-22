package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.AbstractS3KeyManager
import com.saveourtool.save.storage.key.S3KeyManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.PostConstruct

/**
 * A simple implementation of [Storage]
 */
abstract class AbstractSimpleStorage<K : Any>(
    s3Operations: S3Operations,
    prefix: String,
) : AbstractS3Storage<K>(
    s3Operations,
) {
    private val initializer: StorageInitializer = StorageInitializer(this::class)
    override val s3KeyManager: S3KeyManager<K> = object : AbstractS3KeyManager<K>(prefix) {
        override fun buildKeyFromSuffix(s3KeySuffix: String): K = doBuildKeyFromSuffix(s3KeySuffix)
        override fun buildS3KeySuffix(key: K): String = doBuildS3KeySuffix(key)
    }

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    abstract fun doBuildKeyFromSuffix(s3KeySuffix: String): K

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    abstract fun doBuildS3KeySuffix(key: K): String

    /**
     * Init method to call [initializer]
     */
    @PostConstruct
    fun init() {
        initializer.init {
            doInit()
        }
    }

    /**
     * @return result of init method as [Mono] without body, it's [Mono.empty] by default
     */
    protected open fun doInit(): Mono<Unit> = Mono.empty()

    override fun list(): Flux<K> = initializer.validateAndRun { super.list() }

    override fun download(key: K): Flux<ByteBuffer> = initializer.validateAndRun { super.download(key) }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<K> = initializer.validateAndRun { super.upload(key, content) }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> = initializer.validateAndRun { super.upload(key, contentLength, content) }

    override fun delete(key: K): Mono<Boolean> = initializer.validateAndRun { super.delete(key) }

    override fun lastModified(key: K): Mono<Instant> = initializer.validateAndRun { super.lastModified(key) }

    override fun contentLength(key: K): Mono<Long> = initializer.validateAndRun { super.contentLength(key) }

    override fun doesExist(key: K): Mono<Boolean> = initializer.validateAndRun { super.doesExist(key) }

    override fun move(source: K, target: K): Mono<Boolean> = initializer.validateAndRun { super.move(source, target) }

    override fun generateUrlToDownload(key: K): URL = initializer.validateAndRun { super.generateUrlToDownload(key) }
}
