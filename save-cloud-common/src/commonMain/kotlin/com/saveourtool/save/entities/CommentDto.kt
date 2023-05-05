package com.saveourtool.save.entities

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property message
 * @property userName
 * @property userAvatar
 * @property createDate
 * @property section
 */
@Serializable
data class CommentDto(
    val message: String,
    val userName: String,
    val userAvatar: String?,
    val createDate: LocalDateTime?,
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
