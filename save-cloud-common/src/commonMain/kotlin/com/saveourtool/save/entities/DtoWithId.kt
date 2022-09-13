package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property content
 */
@Serializable
data class DtoWithId<T>(
    val id: Long,
    val content: T,
)
