package com.saveourtool.save.entities

import com.saveourtool.save.utils.LocalDateTime
import kotlinx.serialization.Contextual
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
    @Contextual
    val startTime: LocalDateTime,
    @Contextual
    val endTime: LocalDateTime,
    val description: String?,
)
