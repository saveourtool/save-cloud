package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.Metastore
import com.saveourtool.save.storage.key.S3KeyAdapter
import java.net.URL

class DefaultStorage<K : Any>(
    s3Operations: S3Operations,
    s3KeyAdapter: S3KeyAdapter<K>,
    metastore: Metastore<K>?,
) : Storage<K> {
    private val coroutinesImpl = DefaultStorageCoroutines(
        s3Operations,
        s3KeyAdapter,
        metastore,
    )
    private val projectReactorImpl = DefaultStorageProjectReactor(
        s3Operations,
        s3KeyAdapter,
//        metastore,
    )
    private val preSignedUrlImpl = DefaultStoragePreSignedUrl(
        s3Operations,
        s3KeyAdapter,
    )

    override fun withCoroutines(): StorageCoroutines<K> = coroutinesImpl

    override fun withProjectReactor(): StorageProjectReactor<K> = projectReactorImpl

    override fun generateUrlToDownload(key: K): URL = preSignedUrlImpl.generateUrlToDownload(key)

    override fun generateUrlToUpload(key: K, contentLength: Long): UrlWithHeaders = preSignedUrlImpl.generateUrlToUpload(key, contentLength)
}