package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.storage.key.AbstractS3KeyEntityManager

/**
 * Implementation of storage which stores keys ([E]) in database and uses S3 storage under hood
 *
 * @param s3Operations interface to operate with S3 storage
 * @param s3KeyManager [AbstractS3KeyEntityManager] manager for S3 keys using database
 * @param repository repository for [E]
 */
abstract class AbstractStorageWithDatabaseEntityKey<E : BaseEntity, R : BaseEntityRepository<E>, M : AbstractS3KeyEntityManager<E, R>>(
    s3Operations: S3Operations,
    s3KeyManager: M,
    repository: R,
) : AbstractStorageWithDatabase<E, E, R, M>(
    s3Operations,
    s3KeyManager,
    repository,
)
