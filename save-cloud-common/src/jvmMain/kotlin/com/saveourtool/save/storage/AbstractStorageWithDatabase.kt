package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.data.domain.Example
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.PostConstruct

import kotlinx.datetime.Clock

/**
 * Implementation of storage which stores keys in database
 *
 * @property storage some storage which uses [Long] ([BaseEntity.id]) as a key
 * @property backupStorageCreator creator for some storage which uses [Long] as a key, should be unique per each creation (to avoid duplication in backups)
 * @property repository repository for [E]
 */
abstract class AbstractStorageWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    private val storage: Storage<Long>,
    private val backupStorageCreator: () -> Storage<Long>,
    protected val repository: R,
) : Storage<K> {
    private val log: Logger = getLogger(this.javaClass)

    /**
     * Implementation using S3 storage
     *
     * @property s3Operations interface to operate with S3 storage
     * @property prefix a common prefix for all keys in S3 storage for this storage
     * @property repository repository for [E] which is entity for [K]
     */
    constructor(
        s3Operations: S3Operations,
        prefix: String,
        repository: R,
    ) : this(
        storage = defaultS3Storage(s3Operations, prefix),
        backupStorageCreator = { defaultS3Storage(s3Operations, prefix.removeSuffix(PATH_DELIMITER) + "-backup-${Clock.System.now().epochSeconds}") },
        repository = repository,
    )

    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    @PostConstruct
    fun backupUnexpectedIds() {
        if (storage is AbstractMigrationStorage<*, *>) {
            storage.migrateAsync()
        } else {
            Mono.just(Unit)
        }
            .flatMapMany {
                storage.detectAsyncUnexpectedIds(repository)
            }
            .collectList()
            .filter { it.isNotEmpty() }
            .flatMapIterable { unexpectedIds ->
                val backupStorage = backupStorageCreator()
                log.warn {
                    "Found unexpected ids $unexpectedIds in storage ${this::class.simpleName}. Move them to backup storage..."
                }
                generateSequence { backupStorage }.take(unexpectedIds.size)
                    .toList()
                    .zip(unexpectedIds)
            }
            .flatMap { (backupStorage, id) ->
                storage.contentLength(id)
                    .flatMap { contentLength ->
                        backupStorage.upload(id, contentLength, storage.download(id))
                    }
                    .then(storage.delete(id))
            }
            .subscribe()
    }

    /**
     * @return a key [K] created from receiver entity [E]
     */
    protected abstract fun E.toKey(): K

    /**
     * @return an entity [E] created from receiver key [K]
     */
    protected abstract fun K.toEntity(): E

    override fun list(): Flux<K> = blockingToFlux {
        repository.findAll().map { it.toKey() }
    }

    override fun doesExist(key: K): Mono<Boolean> = blockingToMono { findEntity(key) }
        .flatMap { entity ->
            storage.doesExist(entity.requiredId())
                .filter { it }
                .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
                    "The key $key is presented in database, but missed in storage"
                }
        }
        .defaultIfEmpty(false)

    override fun contentLength(key: K): Mono<Long> = getIdAsMono(key).flatMap { storage.contentLength(it) }

    override fun lastModified(key: K): Mono<Instant> = getIdAsMono(key).flatMap { storage.lastModified(it) }

    override fun delete(key: K): Mono<Boolean> = blockingToMono { findEntity(key) }
        .flatMap { entity ->
            storage.delete(entity.requiredId())
                .asyncEffectIf({ this }) {
                    doDelete(entity)
                }
        }
        .defaultIfEmpty(false)

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> = doUpload(key, content).map(Pair<Any, Long>::second)

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> = uploadAndReturnUpdatedKey(key, contentLength, content).thenReturn(Unit)

    /**
     * @param key a key for provided content
     * @param content
     * @return updated key [E]
     */
    open fun uploadAndReturnUpdatedKey(key: K, content: Flux<ByteBuffer>): Mono<K> = doUpload(key, content).map(Pair<K, Any>::first)

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content
     * @return updated key [E]
     */
    open fun uploadAndReturnUpdatedKey(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> = blockingToMono {
        repository.save(key.toEntity())
    }
        .flatMap { entity ->
            storage.upload(entity.requiredId(), contentLength, content)
                .map { entity.toKey() }
                .onErrorResume { ex ->
                    doDelete(entity).then(Mono.error(ex))
                }
        }

    private fun doUpload(key: K, content: Flux<ByteBuffer>): Mono<Pair<K, Long>> = blockingToMono {
        repository.save(key.toEntity())
    }
        .flatMap { entity ->
            storage.upload(entity.requiredId(), content)
                .flatMap { contentSize ->
                    blockingToMono { repository.save(entity.updateByContentSize(contentSize)) }
                        .map {
                            it.toKey() to contentSize
                        }
                }
                .onErrorResume { ex ->
                    doDelete(entity).then(Mono.error(ex))
                }
        }

    override fun move(source: K, target: K): Mono<Boolean> = throw UnsupportedOperationException("${AbstractStorageWithDatabase::class.simpleName} storage doesn't support moving")

    override fun download(key: K): Flux<ByteBuffer> = getIdAsMono(key).flatMapMany { storage.download(it) }

    private fun getIdAsMono(key: K): Mono<Long> = blockingToMono { findEntity(key)?.requiredId() }
        .switchIfEmptyToNotFound { "Key $this is not saved: ID is not set and failed to find by default example" }

    private fun doDelete(entity: E): Mono<Unit> = blockingToMono {
        beforeDelete(entity)
        repository.delete(entity)
    }

    /**
     * A default implementation uses Spring's [Example]
     *
     * @param key
     * @return [E] entity found by [K] key or null
     */
    protected abstract fun findEntity(key: K): E?

    /**
     * @receiver [E] entity which needs to be processed before deletion
     * @param entity
     */
    protected open fun beforeDelete(entity: E): Unit = Unit

    /**
     * @receiver [E] entity which needs to be updated by [sizeBytes]
     * @param sizeBytes
     * @return updated [E] entity
     */
    protected open fun E.updateByContentSize(sizeBytes: Long): E = this

    companion object {
        private fun defaultS3Storage(s3Operations: S3Operations, prefix: String): Storage<Long> = object : AbstractS3Storage<Long>(s3Operations, prefix) {
            override fun buildKey(s3KeySuffix: String): Long = s3KeySuffix.toLong()
            override fun buildS3KeySuffix(key: Long): String = key.toString()
        }
    }
}
