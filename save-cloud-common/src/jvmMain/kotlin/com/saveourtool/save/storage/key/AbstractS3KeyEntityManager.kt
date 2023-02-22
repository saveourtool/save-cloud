package com.saveourtool.save.storage.key

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull

/**
 * Implementation of [S3KeyManager] uses entity [E] as key
 *
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @param repository repository for [E]
 */
abstract class AbstractS3KeyEntityManager<E : BaseEntity, R : BaseEntityRepository<E>>(
    prefix: String,
    repository: R,
) : AbstractS3KeyDatabaseManager<E, E, R>(
    prefix,
    repository,
) {
    override fun convertEntityToKey(entity: E): E = entity

    override fun convertKeyToEntity(key: E): E = key

    override fun doFindEntity(key: E): E? = key.id
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
