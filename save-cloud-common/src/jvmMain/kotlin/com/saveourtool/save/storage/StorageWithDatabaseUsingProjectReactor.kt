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
open class StorageWithDatabaseUsingProjectReactor<K : Any, E : BaseEntity, R : BaseEntityRepository<E>, M : AbstractS3KeyDatabaseManager<K, E, R>>(
    private val s3Operations: S3Operations,
    override val s3KeyManager: M,
    private val repository: R,
) : AbstractStorageUsingProjectReactor<K>(s3Operations) {
    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    override fun doInit(): Mono<Unit> = Mono.fromFuture {
        s3Operations.backupUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            commonPrefix = s3KeyManager.commonPrefix,
        ) { s3Key ->
            val id = s3Key.removePrefix(s3KeyManager.commonPrefix).toLong()
            repository.findById(id).isEmpty
        }
    }.publishOn(s3Operations.scheduler)
}
