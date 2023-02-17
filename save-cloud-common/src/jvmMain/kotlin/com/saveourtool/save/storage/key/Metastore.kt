package com.saveourtool.save.storage.key

import com.saveourtool.save.storage.PATH_DELIMITER

interface Metastore<K : Any> {
    /**
     * a flag that shows if there is a database underlay
     */
    val isDatabaseUnderlying: Boolean

    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
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
     * @param s3Key cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3Key] or null if path invalid
     */
    fun buildKey(s3Key: String): K?

    /**
     * @param key
     * @return s3 key, cannot start with [PATH_DELIMITER]
     */
    fun buildNewS3Key(key: K): String

    /**
     * @param key
     * @return s3 key, cannot start with [PATH_DELIMITER] if metastore contains [key]
     */
    fun buildExistedS3Key(key: K): String?
}
