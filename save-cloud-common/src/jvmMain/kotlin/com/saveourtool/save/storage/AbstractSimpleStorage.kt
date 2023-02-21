package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.AbstractS3KeyManager
import com.saveourtool.save.storage.key.S3KeyManager

/**
 * A simple implementation of [Storage]
 */
abstract class AbstractSimpleStorage<K : Any>(
    s3Operations: S3Operations,
    prefix: String,
) : AbstractStorage<K>(
    s3Operations,
) {
    override val s3KeyManager: S3KeyManager<K> = object : AbstractS3KeyManager<K>(prefix) {
        override fun buildKeyFromSuffix(s3KeySuffix: String): K = doBuildKeyFromSuffix(s3KeySuffix)
        override fun buildS3KeySuffix(key: K): String = doBuildS3KeySuffix(key)
    }

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    abstract fun doBuildKeyFromSuffix(s3KeySuffix: String): K

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    abstract fun doBuildS3KeySuffix(key: K): String
}
