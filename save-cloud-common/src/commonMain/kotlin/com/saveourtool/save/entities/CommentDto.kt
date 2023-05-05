package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * @property message
 * @property userName
 * @property userAvatar
 * @property section
 */
@Serializable
data class CommentDto(
    val message: String,
    val userName: String,
    val userAvatar: String?,
    val createDate: Long?,
    val section: String = "",
) {
    companion object {
        val empty = CommentDto(
            "",
            "Unknown",
            null,
            null,
        )
    }
}
