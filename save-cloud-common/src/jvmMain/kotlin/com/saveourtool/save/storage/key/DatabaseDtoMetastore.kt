package com.saveourtool.save.storage.key

import com.saveourtool.save.entities.DtoWithId
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull

abstract class DatabaseDtoMetastore<K : DtoWithId, E : BaseEntityWithDtoWithId<K>, R : BaseEntityRepository<E>>(
    prefix: String,
    repository: R,
) : AbstractDatabaseMetastore<K, E, R>(prefix, repository) {
    override fun E.toKey(): K = toDto()

    override fun K.toEntity(): E = createNewEntityFromDto(this)

    override fun findEntity(key: K): E? = key.id
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