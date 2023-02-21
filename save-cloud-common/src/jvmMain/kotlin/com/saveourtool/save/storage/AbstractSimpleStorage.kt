package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations

/**
 * S3 implementation of Storage
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param prefix a common prefix for all S3 keys in this storage
 * @param K type of key
 */
abstract class AbstractSimpleStorage<K : Any>(
    private val s3Operations: S3Operations,
    prefix: String,
) : AbstractStorage<K, AbstractSimpleStorageProjectReactor<K>, AbstractSimpleStoragePreSignedUrl<K>>() {
    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
     */
    protected val prefix = prefix.asS3CommonPrefix()
    override val storageProjectReactor: AbstractSimpleStorageProjectReactor<K> = object : AbstractSimpleStorageProjectReactor<K>(s3Operations, prefix) {
        override fun buildKey(s3KeySuffix: String): K = doBuildKey(s3KeySuffix)
        override fun buildS3KeySuffix(key: K): String = doBuildS3KeySuffix(key)
    }
    override val storagePreSignedUrl: AbstractSimpleStoragePreSignedUrl<K> = object : AbstractSimpleStoragePreSignedUrl<K>(s3Operations, prefix) {
        override fun buildS3KeySuffix(key: K): String = doBuildS3KeySuffix(key)
    }

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    protected abstract fun doBuildKey(s3KeySuffix: String): K

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    protected abstract fun doBuildS3KeySuffix(key: K): String
}
