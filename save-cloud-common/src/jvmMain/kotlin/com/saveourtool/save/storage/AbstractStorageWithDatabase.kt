package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.storage.key.AbstractS3KeyDatabaseManager
import com.saveourtool.save.storage.key.AbstractS3KeyManager
import com.saveourtool.save.storage.key.S3KeyManager
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.PostConstruct

import kotlinx.datetime.Clock
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementation of S3 storage which stores keys in database
 *
 * @property s3Operations interface to operate with S3 storage
 * @property s3KeyManager [AbstractS3KeyDatabaseManager] manager for S3 keys using database
 * @property repository repository for [E] which is entity for [K]
 */
abstract class AbstractStorageWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>, M: AbstractS3KeyDatabaseManager<K, E, R>>(
    private val s3Operations: S3Operations,
    protected val s3KeyManager: M,
    protected val repository: R,
) : Storage<K> {
    private val log: Logger = getLogger(this.javaClass)

    private val underlyingStorage = object : AbstractS3Storage<K>(s3Operations) {
        override val s3KeyManager: S3KeyManager<K> = this@AbstractStorageWithDatabase.s3KeyManager
    }

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitStarted = AtomicBoolean(false)

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitFinished = AtomicBoolean(false)

    private val initScheduler: Scheduler = Schedulers.boundedElastic()

    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    @PostConstruct
    fun init() {
        require(!isInitStarted.compareAndExchange(false, true)) {
            "Init method cannot be called more than 1 time, initialization is in progress"
        }
        doBackupUnexpectedIds()
            .doOnNext {
                require(!isInitFinished.compareAndExchange(false, true)) {
                    "Init method cannot be called more than 1 time. Initialization already finished by another project"
                }
                log.info {
                    "Initialization of ${javaClass.simpleName} is done"
                }
            }
            .subscribeOn(initScheduler)
            .subscribe()
    }

    private fun doBackupUnexpectedIds(): Mono<Unit> {
        val directStorage = DirectStorage(s3KeyManager.commonPrefix)
        return directStorage.detectAsyncUnexpectedIds(repository)
            .collectList()
            .filter { it.isNotEmpty() }
            .flatMapIterable { unexpectedIds ->
                val backupStorage = DirectStorage(s3KeyManager.commonPrefix.removeSuffix(PATH_DELIMITER) + "-backup-${Clock.System.now().epochSeconds}")
                log.warn {
                    "Found unexpected ids $unexpectedIds in storage ${this::class.simpleName}. Move them to backup storage..."
                }
                generateSequence { backupStorage }.take(unexpectedIds.size)
                    .toList()
                    .zip(unexpectedIds)
            }
            .flatMap { (backupStorage, id) ->
                directStorage.contentLength(id)
                    .flatMap { contentLength ->
                        backupStorage.upload(id, contentLength, directStorage.download(id))
                    }
                    .then(directStorage.delete(id))
            }
            .collectList()
            .map { results ->
                if (results.isNotEmpty()) {
                    log.info {
                        "Backup of ${javaClass.simpleName} finished"
                    }
                }
            }
    }

    private fun <R> validateAndRun(action: () -> R): R {
        require(isInitFinished.get()) {
            "Any method of ${javaClass.simpleName} should be called after init is finished"
        }
        return action()
    }

    override fun list(): Flux<K> = validateAndRun { underlyingStorage.list() }

    override fun download(key: K): Flux<ByteBuffer> = validateAndRun { underlyingStorage.download(key) }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<KeyWithContentLength<K>> = validateAndRun { underlyingStorage.upload(key, content) }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> = validateAndRun { underlyingStorage.upload(key, contentLength, content) }

    override fun delete(key: K): Mono<Boolean> = validateAndRun { underlyingStorage.delete(key) }

    override fun lastModified(key: K): Mono<Instant> = validateAndRun { underlyingStorage.lastModified(key) }

    override fun contentLength(key: K): Mono<Long> = validateAndRun { underlyingStorage.contentLength(key) }

    override fun doesExist(key: K): Mono<Boolean> = validateAndRun { underlyingStorage.doesExist(key) }

    override fun move(source: K, target: K): Mono<Boolean> = validateAndRun { underlyingStorage.move(source, target) }

    override fun generateUrlToDownload(key: K): URL = validateAndRun { underlyingStorage.generateUrlToDownload(key) }

    private open inner class DirectStorage(underlyingPrefix: String) : AbstractS3Storage<Long>(
        s3Operations,
    ) {
        override val s3KeyManager = object : AbstractS3KeyManager<Long>(underlyingPrefix) {
            override fun buildKeyFromSuffix(s3KeySuffix: String): Long = s3KeySuffix.toLong()
            override fun delete(key: Long) = Unit
            override fun buildS3KeySuffix(key: Long): String = key.toString()
        }
    }
}
