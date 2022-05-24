package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * @property url
 * @property username
 * @property password
 * @property branch
 * @property hash commit hash, null means consumer should use the latest commit
 */
@Serializable
data class GitDto(
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val branch: String? = null,
    val hash: String? = null,
)
