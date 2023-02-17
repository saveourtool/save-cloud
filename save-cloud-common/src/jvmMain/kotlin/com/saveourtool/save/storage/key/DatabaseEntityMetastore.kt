package com.saveourtool.save.storage.key

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull

class DatabaseEntityMetastore<E : BaseEntity, R : BaseEntityRepository<E>>(
    commonPrefix: String,
    repository: R,
    beforeDelete: (E) -> Unit = { },
    private val findByContent: (E) -> E?,
) : AbstractDatabaseMetastore<E, E, R>(
    commonPrefix,
    repository,
    beforeDelete,
) {
    override fun E.toKey(): E = this

    override fun E.toEntity(): E = this

    override fun findEntity(key: E): E? = key.id
        ?.let { id ->
            repository.findByIdOrNull(id)
                .orNotFound { "Failed to find entity for $this by id = $id" }
        }
        ?: findByContent(key)
}
