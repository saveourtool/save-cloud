package com.saveourtool.save.storage

import com.saveourtool.save.entities.DtoWithId
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.storage.key.AbstractS3KeyDatabaseManager
import com.saveourtool.save.storage.key.AbstractS3KeyDtoManager
import com.saveourtool.save.utils.*

import org.springframework.data.repository.findByIdOrNull

/**
 * Implementation of storage which stores keys ([K]) in database and uses S3 storage under hood
 *
 * @param s3Operations interface to operate with S3 storage
 * @param s3KeyManager [AbstractS3KeyDtoManager] manager for S3 keys using database
 * @param repository repository for [E] which is entity for [K]
 */
abstract class AbstractStorageWithDatabaseDtoKey<K : DtoWithId, E : BaseEntityWithDtoWithId<K>, R : BaseEntityRepository<E>, M : AbstractS3KeyDtoManager<K, E, R>>(
    s3Operations: S3Operations,
    s3KeyManager: M,
    repository: R,
) : AbstractStorageWithDatabase<K, E, R, M>(
    s3Operations,
    s3KeyManager,
    repository,
)
