package com.saveourtool.save.storage.key

import com.saveourtool.save.storage.PATH_DELIMITER

/**
 * A common implementation for [Metastore]
 *
 * @param prefix a common prefix for all S3 keys in this storage
 */
abstract class AbstractMetastore<K : Any>(
    prefix: String,
) : Metastore<K> {
    private val s3KeyAdapter: S3KeyAdapter<K> = object : AbstractS3KeyAdapter<K>(prefix) {
        override fun buildKeyFromSuffix(s3KeySuffix: String): K {
            return this@AbstractMetastore.buildKeyFromSuffix(s3KeySuffix)
        }
        override fun buildS3KeySuffix(key: K): String {
            return this@AbstractMetastore.buildS3KeySuffix(key)
        }
    }

    final override val commonPrefix: String = s3KeyAdapter.commonPrefix

    override fun buildKey(s3Key: String): K? = s3KeyAdapter.buildKey(s3Key)

    override fun buildNewS3Key(key: K): String = s3KeyAdapter.buildS3Key(key)

    override fun buildExistedS3Key(key: K): String? = s3KeyAdapter.buildS3Key(key)

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    protected abstract fun buildKeyFromSuffix(s3KeySuffix: String): K

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    protected abstract fun buildS3KeySuffix(key: K): String
}