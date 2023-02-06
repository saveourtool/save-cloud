package com.saveourtool.save.storage

import com.saveourtool.save.entities.DtoWithId
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*

import org.springframework.data.repository.findByIdOrNull

/**
 * Implementation of storage which stores keys ([K]) in database and uses S3 storage under hood
 *
 * @param s3Operations interface to operate with S3 storage
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @param repository repository for [E] which is entity for [K]
 */
abstract class AbstractStorageWithDatabaseDtoKey<K : DtoWithId, E : BaseEntityWithDtoWithId<K>, R : BaseEntityRepository<E>>(
    s3Operations: S3Operations,
    prefix: String,
    repository: R,
) : AbstractStorageWithDatabase<K, E, R>(
    s3Operations,
    prefix,
    repository,
) {
    override fun E.toKey(): K = toDto()

    override fun K.toEntity(): E = createNewEntityFromDto(this)

    final override fun findEntity(key: K): E? = key.id
        ?.let { id ->
            repository.findByIdOrNull(id)
                .orNotFound { "Failed to find entity for $this by id = $id" }
        }
        ?: findByDto(key)

    /**
     * @param dto
     * @return [E] entity found by [K] dto or null
     */
    protected abstract fun findByDto(dto: K): E?

    /**
     * @param dto
     * @return a new [E] entity is created from provided [K] dto
     */
    abstract fun createNewEntityFromDto(dto: K): E
}
