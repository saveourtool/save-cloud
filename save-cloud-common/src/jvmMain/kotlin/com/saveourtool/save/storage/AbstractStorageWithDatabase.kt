package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository

import org.springframework.data.domain.Example
import reactor.core.publisher.Mono

/**
 * Implementation of S3 storage which stores keys in database
 *
 * @param s3Operations interface to operate with S3 storage
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @property repository repository for [E] which is entity for [K]
 */
abstract class AbstractStorageWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    private val s3Operations: S3Operations,
    private val prefix: String,
    protected val repository: R,
) : AbstractStorage<K, AbstractStorageProjectReactorWithDatabase<K, E, R>, AbstractStoragePreSignedWithDatabase<K, E, R>>() {
    private val commonPrefix: String = prefix.asS3CommonPrefix()
    private val underlyingStorageProjectReactor = defaultStorageProjectReactor(s3Operations, commonPrefix)
    private val underlyingStoragePreSignedUrl = defaultStoragePreSignedUrl(s3Operations, commonPrefix)
    override val storageProjectReactor = object : AbstractStorageProjectReactorWithDatabase<K, E, R>(
        underlyingStorageProjectReactor,
        repository,
    ) {
        override fun E.toKey(): K = convertEntityToKey(this)

        override fun K.toEntity(): E = convertKeyToEntity(this)

        override fun findEntity(key: K): E? = doFindEntity(key)

        override fun beforeDelete(entity: E) = doBeforeDelete(entity)
    }
    override val storagePreSignedUrl = object : AbstractStoragePreSignedWithDatabase<K, E, R>(
        underlyingStoragePreSignedUrl,
        repository,
    ) {
        override fun findEntity(key: K): E? = doFindEntity(key)
    }

    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    override fun doInitAsync(storageProjectReactor: AbstractStorageProjectReactorWithDatabase<K, E, R>): Mono<Unit> = Mono.fromFuture {
        s3Operations.backupUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            commonPrefix = commonPrefix,
        ) { s3Key ->
            val id = s3Key.removePrefix(commonPrefix).toLong()
            repository.findById(id).isEmpty
        }
    }.publishOn(s3Operations.scheduler)

    /**
     * @param entity
     * @return a key [K] created from receiver entity [E]
     */
    protected abstract fun convertEntityToKey(entity: E): K

    /**
     * @param key
     * @return an entity [E] created from receiver key [K]
     */
    protected abstract fun convertKeyToEntity(key: K): E

    /**
     * A default implementation uses Spring's [Example]
     *
     * @param key
     * @return [E] entity found by [K] key or null
     */
    protected abstract fun doFindEntity(key: K): E?

    /**
     * @receiver [E] entity which needs to be processed before deletion
     * @param entity
     */
    protected open fun doBeforeDelete(entity: E): Unit = Unit

    companion object {
        private fun defaultStorageProjectReactor(s3Operations: S3Operations, prefix: String): StorageProjectReactor<Long> = object : AbstractSimpleStorageProjectReactor<Long>(
            s3Operations, prefix
        ) {
            override fun buildKey(s3KeySuffix: String): Long = s3KeySuffix.toLong()
            override fun buildS3KeySuffix(key: Long): String = key.toString()
        }
        private fun defaultStoragePreSignedUrl(s3Operations: S3Operations, prefix: String): StoragePreSignedUrl<Long> = object : AbstractSimpleStoragePreSignedUrl<Long>(
            s3Operations
        ) {
            override fun buildS3KeySuffix(key: Long): String = key.toString()
        }
    }
}
