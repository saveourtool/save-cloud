package com.saveourtool.save.storage

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*

import org.springframework.data.domain.Example

import java.net.URL

/**
 * Implementation of storage which stores keys in database
 *
 * @property storagePreSignedUrl some [StoragePreSignedUrl] which uses [Long] ([BaseEntity.id]) as a key
 * @property repository repository for [E]
 */
abstract class AbstractStoragePreSignedWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    private val storagePreSignedUrl: StoragePreSignedUrl<Long>,
    protected val repository: R,
) : StoragePreSignedUrl<K> {
    override fun generateUrlToDownload(key: K): URL = getId(key).let { storagePreSignedUrl.generateUrlToDownload(it) }

    private fun getId(key: K): Long = findEntity(key)?.requiredId().orNotFound { "Key $key is not saved: ID is not set and failed to find by default example" }

    /**
     * A default implementation uses Spring's [Example]
     *
     * @param key
     * @return [E] entity found by [K] key or null
     */
    protected abstract fun findEntity(key: K): E?
}
