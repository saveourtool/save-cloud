package com.saveourtool.save.storage.key

import com.saveourtool.save.entities.DtoWithId
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull

class DatabaseDtoMetastore<K : DtoWithId, E : BaseEntityWithDtoWithId<K>, R : BaseEntityRepository<E>>(
    commonPrefix: String,
    repository: R,
    beforeDelete: (E) -> Unit = { },
    private val findByDto: (K) -> E?,
    private val createNewEntityFromDto: (K) -> E,
) : AbstractDatabaseMetastore<K, E, R>(commonPrefix, repository, beforeDelete) {
    override fun E.toKey(): K = toDto()

    override fun K.toEntity(): E = createNewEntityFromDto(this)

    override fun findEntity(key: K): E? = key.id
        ?.let { id ->
            repository.findByIdOrNull(id)
                .orNotFound { "Failed to find entity for $this by id = $id" }
        }
        ?: findByDto(key)
}