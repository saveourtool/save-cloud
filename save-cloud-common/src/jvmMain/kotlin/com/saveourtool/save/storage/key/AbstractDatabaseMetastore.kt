package com.saveourtool.save.storage.key

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.isNotNull
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull

abstract class AbstractDatabaseMetastore<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    override val commonPrefix: String,
    protected val repository: R,
    private val beforeDelete: (E) -> Unit,
) : AbstractS3KeyAdapter<K>(commonPrefix), Metastore<K> {

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

    override fun list(): Collection<K> {
        return repository.findAll().map { it.toKey() }
    }

    /**
     * @param key
     * @return true if metastore contains [key]
     */
    override fun contains(key: K): Boolean = findEntity(key).isNotNull()

    override fun delete(key: K) {
        findEntity(key)?.let { entity ->
            beforeDelete(entity)
            repository.delete(entity)
        }
    }

    override fun save(key: K): K = repository.save(key.toEntity()).toKey()

    override fun buildExistedS3Key(key: K): String? = findEntity(key)?.requiredId()?.toString()

    override fun buildKeyFromSuffix(s3KeySuffix: String): K = repository.findByIdOrNull(s3KeySuffix.toLong())
        .orNotFound {
            "Not found entity by id $s3KeySuffix"
        }
        .toKey()

    override fun buildS3KeySuffix(key: K): String {
        val entity = repository.save(key.toEntity())
        return entity.requiredId().toString()
    }
}
