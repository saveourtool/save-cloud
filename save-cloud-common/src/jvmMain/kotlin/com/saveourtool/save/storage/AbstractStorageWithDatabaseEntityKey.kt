package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*

import org.springframework.data.repository.findByIdOrNull

/**
 * Implementation of storage which stores keys ([E]) in database and uses S3 storage under hood
 *
 * @param s3Operations interface to operate with S3 storage
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @param repository repository for [E]
 */
abstract class AbstractStorageWithDatabaseEntityKey<E : BaseEntity, R : BaseEntityRepository<E>>(
    s3Operations: S3Operations,
    prefix: String,
    repository: R,
) : AbstractStorageWithDatabase<E, E, R>(
    s3Operations,
    prefix,
    repository,
) {
    override fun convertEntityToKey(entity: E): E = entity

    override fun convertKeyToEntity(key: E): E = key

    final override fun doFindEntity(key: E): E? = key.id
        ?.let { id ->
            repository.findByIdOrNull(id)
                .orNotFound { "Failed to find entity for $this by id = $id" }
        }
        ?: findByContent(key)

    /**
     * @param key
     * @return [E] entity found in database by [E] or null
     */
    protected abstract fun findByContent(key: E): E?
}
