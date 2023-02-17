package com.saveourtool.save.storage.key

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.isNotNull
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

abstract class AbstractDatabaseMetastore<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    prefix: String,
    protected val repository: R,
) : AbstractMetastore<K>(prefix), Metastore<K> {
    override val isDatabaseUnderlying: Boolean = true

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
    override fun buildNewS3Key(key: K): String {
        return super.buildNewS3Key(key)
    }

    override fun buildExistedS3Key(key: K): String? = findEntity(key)?.let { super.buildExistedS3Key(key) }

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
