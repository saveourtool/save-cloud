@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * Interface that unified [SandboxFileInfo] and [FileInfo]
 */
sealed interface AbstractFileInfo {
    /**
     * Size in bytes
     */
    val sizeBytes: Long

    /**
     * Name of a file
     */
    val name: String
}

/**
 * @property key [FileKey] of this [FileInfo]
 * @property sizeBytes size in bytes
 * @property isExecutable
 */
@Serializable
data class FileInfo(
    val key: FileKey,
    override val sizeBytes: Long,
    val isExecutable: Boolean = false,
) : AbstractFileInfo {
    override val name: String = key.name
}

/**
 * Class that contains metadata of Sandbox files
 *
 * @property sizeBytes size in bytes
 * @property name name of a file
 */
@Serializable
data class SandboxFileInfo(
    override val name: String,
    override val sizeBytes: Long,
) : AbstractFileInfo
