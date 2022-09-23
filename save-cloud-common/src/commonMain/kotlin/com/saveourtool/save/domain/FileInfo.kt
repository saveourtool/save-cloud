@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * @property key [FileKey] of this [FileInfo]
 * @property sizeBytes size in bytes
 * @property isExecutable
 */
@Serializable
data class FileInfo(
    val key: FileKey,
    val sizeBytes: Long,
    val isExecutable: Boolean = false,
)
