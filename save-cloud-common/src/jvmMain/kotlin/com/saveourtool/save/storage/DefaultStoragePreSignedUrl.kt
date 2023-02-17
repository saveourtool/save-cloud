package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.Metastore
import com.saveourtool.save.storage.key.NoMetastore
import com.saveourtool.save.storage.key.S3KeyAdapter

import java.net.URL

import kotlin.time.Duration.Companion.minutes

/**
 * S3 implementation of [StoragePreSignedUrl]
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param metastore [Metastore] metastore with S3 keys
 * @param K type of key
 */
class DefaultStoragePreSignedUrl<K : Any>(
    private val s3Operations: S3Operations,
    private val metastore: Metastore<K>,
) : StoragePreSignedUrl<K> {
    override fun generateUrlToDownload(key: K): URL? =
        metastore.buildExistedS3Key(key)?.let { s3Key ->
            s3Operations.requestToDownloadObject(s3Key, presignedDuration)
                .also { request ->
                    require(request.isBrowserExecutable) {
                        "Pre-singer url to download object should be browser executable (header-less)"
                    }
                }
                .url()
        }

    override fun generateUrlToUpload(key: K, contentLength: Long): UrlWithHeaders {
        require(metastore is NoMetastore) {
            "Pre singed URL to upload supported only for ${NoMetastore::class.simpleName}"
        }
        return s3Operations.requestToUploadObject(metastore.buildNewS3Key(key), contentLength, presignedDuration)
            .also { request ->
                require(request.signedPayload().isEmpty) {
                    "Pre-singer url to download object should be without payload"
                }
            }
            .let {
                it.url() to it.signedHeaders()
            }
    }

    companion object {
        private val presignedDuration = 15.minutes
    }
}
