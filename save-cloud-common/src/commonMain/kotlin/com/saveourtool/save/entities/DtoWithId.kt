package com.saveourtool.save.entities

/**
 * @property id
 * @property content
 */
data class DtoWithId<T>(
    val id: Long,
    val content: T,
)
