package com.saveourtool.save.storage.key

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.isNotNull
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

/**
 * Implementation of [S3KeyManager] which stores keys in database
 *
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @property repository repository for [E]
 */
abstract class AbstractS3KeyDatabaseManager<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    prefix: String,
    protected val repository: R,
) : AbstractS3KeyManager<K>(prefix) {
    /**
     * @return a key [K] created from receiver entity [E]
     */
    protected abstract fun E.toKey(): K

    /**
     * @return an entity [E] created from receiver key [K]
     */
    protected abstract fun K.toEntity(): E

    /**
     * @param key
     * @return [E] entity found by [K] key or null
     */
    protected abstract fun findEntity(key: K): E?

    /**
     * @param key
     * @return true if metastore contains [key]
     */
    override fun contains(key: K): Boolean = findEntity(key).isNotNull()

    @Transactional
    override fun delete(key: K) {
        findEntity(key)?.let { entity ->
            beforeDelete(entity)
            repository.delete(entity)
        }
    }

    @Transactional
    override fun createNewS3Key(key: K): String {
        return super.createNewS3Key(key)
    }

    override fun findExistedS3Key(key: K): String? = findEntity(key)?.let { super.findExistedS3Key(key) }

    override fun buildKeyFromSuffix(s3KeySuffix: String): K = repository.findByIdOrNull(s3KeySuffix.toLong())
        .orNotFound {
            "Not found entity by id $s3KeySuffix"
        }
        .toKey()

    override fun buildS3KeySuffix(key: K): String {
        val entity = repository.save(key.toEntity())
        return entity.requiredId().toString()
    }

    /**
     * @receiver [E] entity which needs to be processed before deletion
     * @param entity
     */
    protected open fun beforeDelete(entity: E): Unit = Unit
}
