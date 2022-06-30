package com.saveourtool.save.entities

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Data class of contest information
 *
 * @property name contest name
 * @property description info about contest
 * @property startTime start time of a contest
 * @property endTime end time of a contest
 */
@Serializable
data class ContestDto(
    val name: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val description: String?,
)
