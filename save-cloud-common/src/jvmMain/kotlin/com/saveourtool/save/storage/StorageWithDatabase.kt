package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.storage.key.AbstractS3KeyDatabaseManager
import com.saveourtool.save.storage.key.S3KeyManager
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.PostConstruct

/**
 * Implementation of S3 storage which stores keys in database
 *
 * @property s3Operations interface to operate with S3 storage
 * @property s3KeyManager [AbstractS3KeyDatabaseManager] manager for S3 keys using database
 * @property repository repository for [E] which is entity for [K]
 */
open class StorageWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>, M : AbstractS3KeyDatabaseManager<K, E, R>>(
    private val s3Operations: S3Operations,
    protected val s3KeyManager: M,
    private val repository: R,
) : Storage<K> {
    private val log: Logger = getLogger(this.javaClass)
    private val underlyingStorage = object : AbstractS3Storage<K>(s3Operations) {
        override val s3KeyManager: S3KeyManager<K> = this@StorageWithDatabase.s3KeyManager
    }
    private val initializer = StorageInitializer(this::class)

    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    @PostConstruct
    fun init() {
        initializer.init {
            doBackupUnexpectedIds()
        }
    }

    private fun doBackupUnexpectedIds(): Mono<Unit> = Mono.fromFuture {
        s3Operations.backupUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            commonPrefix = s3KeyManager.commonPrefix,
        ) { s3Key ->
            val id = s3Key.removePrefix(s3KeyManager.commonPrefix).toLong()
            repository.findById(id).isEmpty
        }
    }.publishOn(s3Operations.scheduler)

    override fun list(): Flux<K> = initializer.validateAndRun { underlyingStorage.list() }

    override fun download(key: K): Flux<ByteBuffer> = initializer.validateAndRun { underlyingStorage.download(key) }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<K> = initializer.validateAndRun { underlyingStorage.upload(key, content) }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> = initializer.validateAndRun { underlyingStorage.upload(key, contentLength, content) }

    override fun delete(key: K): Mono<Boolean> = initializer.validateAndRun { underlyingStorage.delete(key) }

    override fun lastModified(key: K): Mono<Instant> = initializer.validateAndRun { underlyingStorage.lastModified(key) }

    override fun contentLength(key: K): Mono<Long> = initializer.validateAndRun { underlyingStorage.contentLength(key) }

    override fun doesExist(key: K): Mono<Boolean> = initializer.validateAndRun { underlyingStorage.doesExist(key) }

    override fun move(source: K, target: K): Mono<Boolean> = initializer.validateAndRun { underlyingStorage.move(source, target) }

    override fun generateUrlToDownload(key: K): URL = initializer.validateAndRun { underlyingStorage.generateUrlToDownload(key) }
}
