package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import java.net.URL
import kotlin.time.Duration.Companion.minutes

/**
 * S3 implementation of [StoragePreSignedUrl]
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param prefix a common prefix for all S3 keys in this storage
 * @param K type of key
 */
abstract class AbstractSimpleStoragePreSignedUrl<K>(
    private val s3Operations: S3Operations,
    prefix: String,
) : StoragePreSignedUrl<K> {
    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
     */
    protected val prefix: String = prefix.removeSuffix(PATH_DELIMITER) + PATH_DELIMITER

    override fun generateUrlToDownload(key: K): URL = s3Operations.requestToDownloadObject(buildS3Key(key), downloadDuration)
        .also { request ->
            require(request.isBrowserExecutable) {
                "Pre-singer url to download object should be browser executable (header-less)"
            }
        }
        .url()

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    protected abstract fun buildS3KeySuffix(key: K): String

    private fun buildS3Key(key: K) = prefix + buildS3KeySuffix(key).validateSuffix()

    companion object {
        private val downloadDuration = 15.minutes
        private fun String.validateSuffix(): String = also { suffix ->
            require(!suffix.startsWith(PATH_DELIMITER)) {
                "Suffix cannot start with $PATH_DELIMITER: $suffix"
            }
        }
    }
}
