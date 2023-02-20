package com.saveourtool.save.storage.key

/**
 * Manager for keys: S3 Key to [K] and vice versa
 */
interface S3KeyManager<K : Any> {
    /**
     * A common prefix endings with [com.saveourtool.save.storage.PATH_DELIMITER] for all s3 keys in this storage
     */
    val commonPrefix: String

    /**
     * @param key
     * @return true if metastore contains [key]
     */
    fun contains(key: K): Boolean

    /**
     * @param key key which needs to delete
     */
    fun delete(key: K)

    /**
     * @param s3Key cannot start with [com.saveourtool.save.storage.PATH_DELIMITER]
     * @return [K] is built from [s3Key] or null if path invalid
     */
    fun findKey(s3Key: String): K?

    /**
     * @param key
     * @return s3 key, cannot start with [com.saveourtool.save.storage.PATH_DELIMITER]
     */
    fun createNewS3Key(key: K): String

    /**
     * @param key
     * @return s3 key, cannot start with [com.saveourtool.save.storage.PATH_DELIMITER] if s3 key valid
     */
    fun findExistedS3Key(key: K): String?
}
