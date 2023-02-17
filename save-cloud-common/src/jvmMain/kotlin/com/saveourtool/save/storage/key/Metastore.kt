package com.saveourtool.save.storage.key

import com.saveourtool.save.storage.PATH_DELIMITER
import org.springframework.transaction.annotation.Transactional

interface Metastore<K : Any> : S3KeyAdapter<K> {
    /**
     * @return all keys
     */
    fun list(): Collection<K>

    /**
     * @param key
     * @return true if metastore contains [key]
     */
    fun contains(key: K): Boolean

    /**
     * @param key key which needs to be saved
     * @return saved key
     */
    fun save(key: K): K

    /**
     * @param key key which needs to delete
     */
    @Transactional
    fun delete(key: K)

    @Transactional
    override fun buildS3Key(key: K): String

    /**
     * @param key
     * @return s3 key, cannot start with [PATH_DELIMITER] if metastore contains [key]
     */
    fun buildExistedS3Key(key: K): String?
}
