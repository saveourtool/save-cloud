package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * Class that contains metadata of Sandbox files
 *
 * @property sizeBytes size in bytes
 * @property name name of a file
 */
@Serializable
data class SandboxFileInfo(
    val name: String,
    val sizeBytes: Long,
)
