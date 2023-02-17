package com.saveourtool.save.storage.key

class NoMetastore<K : Any>(
    private val s3KeyAdapter: S3KeyAdapter<K>,
) : Metastore<K> {
    override val isDatabaseUnderlying: Boolean = false
    override val commonPrefix: String = s3KeyAdapter.commonPrefix

    override fun contains(key: K): Boolean = true

    override fun delete(key: K) = Unit

    override fun buildNewS3Key(key: K): String = s3KeyAdapter.buildS3Key(key)

    override fun buildExistedS3Key(key: K): String = s3KeyAdapter.buildS3Key(key)
}
