package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.storage.key.AbstractS3KeyDatabaseManager

import reactor.core.publisher.Mono

/**
 * Implementation of S3 storage which stores keys in database
 *
 * @property s3Operations interface to operate with S3 storage
 * @property s3KeyManager [AbstractS3KeyDatabaseManager] manager for S3 keys using database
 * @property repository repository for [E] which is entity for [K]
 */
open class ReactiveStorageWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>, M : AbstractS3KeyDatabaseManager<K, E, R>>(
    private val s3Operations: S3Operations,
    override val s3KeyManager: M,
    private val repository: R,
) : AbstractReactiveStorage<K>(s3Operations) {
    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<K>): Mono<Unit> = Mono.fromFuture {
        s3Operations.backupUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            commonPrefix = s3KeyManager.commonPrefix,
        ) { s3Key ->
            val id = s3Key.removePrefix(s3KeyManager.commonPrefix).toLong()
            repository.findById(id).isEmpty
        }
    }.publishOn(s3Operations.scheduler)
}
