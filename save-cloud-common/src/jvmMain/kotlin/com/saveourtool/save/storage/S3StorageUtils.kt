/**
 * Utilities for [Storage] which implements [AbstractS3Storage]
 */

package com.saveourtool.save.storage

/**
 * Delimiter for S3 key
 */
const val PATH_DELIMITER = "/"


/**
 * @param prefix should not end with [PATH_DELIMITER] -- will be deleted
 * @param suffix should not start with [PATH_DELIMITER] -- will be deleted
 * @return a s3 key by concat [prefix] and [suffix] and a single [PATH_DELIMITER] between them
 */
fun concatS3Key(prefix: String, suffix: String): String =
    "${prefix.removeSuffix(PATH_DELIMITER)}$PATH_DELIMITER${suffix.removePrefix(PATH_DELIMITER)}"
