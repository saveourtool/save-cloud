@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.domain

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
) {
    /**
     * @param projectCoordinates
     * @return [FileKey]
     */
    fun toStorageKey(projectCoordinates: ProjectCoordinates) = FileKey(
        projectCoordinates,
        this.name,
        this.uploadedMillis,
    )
}

/**
 * @property path path to image
 */
@Serializable
data class ImageInfo(
    val path: String?,
)
