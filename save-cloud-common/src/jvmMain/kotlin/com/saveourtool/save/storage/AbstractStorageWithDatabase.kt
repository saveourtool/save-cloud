package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*

import org.springframework.data.domain.Example
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer

import kotlinx.datetime.Clock

/**
 * Implementation of storage which stores keys in database
 *
 * @property underlyingStorageProjectReactor some storage which uses [Long] ([BaseEntity.id]) as a key
 * @property backupUnderlyingStorageProjectReactorCreator creator for some storage which uses [Long] as a key, should be unique per each creation (to avoid duplication in backups)
 * @property underlyingStoragePreSignedUrl some [StoragePreSignedUrl] which uses [Long] ([BaseEntity.id]) as a key
 * @property repository repository for [E]
 */
abstract class AbstractStorageWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    private val underlyingStorageProjectReactor: StorageProjectReactor<Long>,
    private val backupUnderlyingStorageProjectReactorCreator: () -> StorageProjectReactor<Long>,
    private val underlyingStoragePreSignedUrl: StoragePreSignedUrl<Long>,
    protected val repository: R,
) : AbstractStorage<K, AbstractStorageProjectReactorWithDatabase<K, E, R>, AbstractStoragePreSignedWithDatabase<K, E, R>>() {
    override val storageProjectReactor = object : AbstractStorageProjectReactorWithDatabase<K, E, R>(
        underlyingStorageProjectReactor,
        repository,
    ) {
        override fun E.toKey(): K = with(this@AbstractStorageWithDatabase) {
            this@toKey.toKey()
        }

        override fun K.toEntity(): E = with(this@AbstractStorageWithDatabase) {
            this@toEntity.toEntity()
        }

        override fun findEntity(key: K): E? = this@AbstractStorageWithDatabase.findEntity(key)

        override fun beforeDelete(entity: E) = this@AbstractStorageWithDatabase.beforeDelete(entity)

        override fun E.updateByContentSize(sizeBytes: Long): E = with(this@AbstractStorageWithDatabase) {
            this@updateByContentSize.updateByContentSize(sizeBytes)
        }
    }
    override val storagePreSignedUrl = object : AbstractStoragePreSignedWithDatabase<K, E, R>(
        underlyingStoragePreSignedUrl,
        repository,
    ) {
        override fun findEntity(key: K): E? = this@AbstractStorageWithDatabase.findEntity(key)
    }

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
        underlyingStorageProjectReactor = defaultStorageProjectReactor(s3Operations, prefix),
        backupUnderlyingStorageProjectReactorCreator = {
            defaultStorageProjectReactor(s3Operations,
                prefix.removeSuffix(PATH_DELIMITER) + "-backup-${Clock.System.now().epochSeconds}")
        },
        underlyingStoragePreSignedUrl = defaultStoragePreSignedUrl(s3Operations, prefix),
        repository = repository,
    )

    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    override fun doInitAsync(storageProjectReactor: AbstractStorageProjectReactorWithDatabase<K, E, R>): Mono<Unit> = underlyingStorageProjectReactor.detectAsyncUnexpectedIds(
        repository
    )
        .collectList()
        .filter { it.isNotEmpty() }
        .flatMapIterable { unexpectedIds ->
            val backupStorage = backupUnderlyingStorageProjectReactorCreator()
            log.warn {
                "Found unexpected ids $unexpectedIds in storage ${this::class.simpleName}. Move them to backup storage..."
            }
            generateSequence { backupStorage }.take(unexpectedIds.size)
                .toList()
                .zip(unexpectedIds)
        }
        .flatMap { (backupStorage, id) ->
            underlyingStorageProjectReactor.contentLength(id)
                .flatMap { contentLength ->
                    backupStorage.upload(id, contentLength, underlyingStorageProjectReactor.download(id))
                }
                .then(underlyingStorageProjectReactor.delete(id))
        }
        .thenJust(Unit)

    /**
     * @return a key [K] created from receiver entity [E]
     */
    protected abstract fun E.toKey(): K

    /**
     * @return an entity [E] created from receiver key [K]
     */
    protected abstract fun K.toEntity(): E

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

    /**
     * @param key a key for provided content
     * @param content
     * @return updated key [E]
     */
    open fun uploadAndReturnUpdatedKey(key: K, content: Flux<ByteBuffer>): Mono<K> = usingProjectReactor().uploadAndReturnUpdatedKey(key, content)

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content
     * @return updated key [E]
     */
    open fun uploadAndReturnUpdatedKey(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> = usingProjectReactor().uploadAndReturnUpdatedKey(key, contentLength,
        content)

    companion object {
        private fun defaultStorageProjectReactor(s3Operations: S3Operations, prefix: String): StorageProjectReactor<Long> = object : AbstractSimpleStorageProjectReactor<Long>(
            s3Operations, prefix
        ) {
            override fun buildKey(s3KeySuffix: String): Long = s3KeySuffix.toLong()
            override fun buildS3KeySuffix(key: Long): String = key.toString()
        }
        private fun defaultStoragePreSignedUrl(s3Operations: S3Operations, prefix: String): StoragePreSignedUrl<Long> = object : AbstractSimpleStoragePreSignedUrl<Long>(
            s3Operations, prefix
        ) {
            override fun buildS3KeySuffix(key: Long): String = key.toString()
        }
    }
}
