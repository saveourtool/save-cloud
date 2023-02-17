package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.Metastore
import com.saveourtool.save.storage.key.S3KeyAdapter
import java.net.URL

open class DefaultStorage<K : Any>(
    s3Operations: S3Operations,
    metastore: Metastore<K>,
) : Storage<K> {
    private val coroutinesImpl = DefaultStorageCoroutines(
        s3Operations,
        metastore,
    )
    private val projectReactorImpl = DefaultStorageProjectReactor(
        s3Operations,
        metastore,
    )
    private val preSignedUrlImpl = DefaultStoragePreSignedUrl(
        s3Operations,
        metastore,
    )

    override fun withCoroutines(): StorageCoroutines<K> = coroutinesImpl

    override fun withProjectReactor(): StorageProjectReactor<K> = projectReactorImpl

    override fun generateUrlToDownload(key: K): URL = preSignedUrlImpl.generateUrlToDownload(key)

    override fun generateUrlToUpload(key: K, contentLength: Long): UrlWithHeaders = preSignedUrlImpl.generateUrlToUpload(key, contentLength)
}