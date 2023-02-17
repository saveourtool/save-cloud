package com.saveourtool.save.storage.key

import com.saveourtool.save.storage.PATH_DELIMITER

interface S3KeyAdapter<K : Any> {
    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
     */
    val commonPrefix: String

    /**
     * @param s3Key cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3Key]
     */
    fun buildKey(s3Key: String): K

    /**
     * @param key
     * @return s3 key, cannot start with [PATH_DELIMITER]
     */
    fun buildS3Key(key: K): String
}