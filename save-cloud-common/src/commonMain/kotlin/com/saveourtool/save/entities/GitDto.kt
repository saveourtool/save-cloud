package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * @property url
 * @property username
 * @property password
 */
@Serializable
data class GitDto(
    val url: String,
    val username: String? = null,
    val password: String? = null,
)
