package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * @property name name of a file
 * @property uploadedMillis timestamp of file uploading
 * @property sizeBytes size in bytes
 * @property isExecutable
 */
@Serializable
data class FileInfo(
    val name: String,
    val uploadedMillis: Long,
    val sizeBytes: Long,
    val isExecutable: Boolean = false,
)

/**
 * @property name name of a file
 * @property base64 byte string of image
 * @property sizeBytes size in bytes
 */
@Serializable
data class ImageInfo(
    val name: String?,
    val base64: String?,
    val sizeBytes: Long?,
)
