package com.saveourtool.save.storage.key

import com.saveourtool.save.storage.PATH_DELIMITER

/**
 * A common implementation for [S3KeyAdapter]
 *
 * @param prefix a common prefix for all S3 keys in this storage
 */
abstract class AbstractS3KeyAdapter<K : Any>(
    prefix: String,
) : S3KeyAdapter<K> {

    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
     */
    protected val prefix: String = prefix.removeSuffix(PATH_DELIMITER) + PATH_DELIMITER


    /**
     * @param s3Key cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3Key]
     */
    final override fun buildKey(s3Key: String): K = buildKeyFromSuffix(s3Key.removePrefix(prefix))

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    protected abstract fun buildKeyFromSuffix(s3KeySuffix: String): K

    final override fun buildS3Key(key: K): String = prefix + buildS3KeySuffix(key).validateSuffix()

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    protected abstract fun buildS3KeySuffix(key: K): String

    companion object {
        private fun String.validateSuffix(): String = also { suffix ->
            require(!suffix.startsWith(PATH_DELIMITER)) {
                "Suffix cannot start with $PATH_DELIMITER: $suffix"
            }
        }
    }
}