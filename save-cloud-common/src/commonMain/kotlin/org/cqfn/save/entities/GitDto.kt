package org.cqfn.save.entities

import kotlinx.serialization.Serializable

@Serializable
data class GitDto(
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val branch: String? = null,
    val project: Project
)
