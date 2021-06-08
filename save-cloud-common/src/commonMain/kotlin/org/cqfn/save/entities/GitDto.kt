package org.cqfn.save.entities

import kotlinx.serialization.Serializable

/**
 * @property url
 * @property username
 * @property password
 * @property branch
 * @property project
 */
@Serializable
data class GitDto(
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val branch: String? = null,
    val project: Project
)
