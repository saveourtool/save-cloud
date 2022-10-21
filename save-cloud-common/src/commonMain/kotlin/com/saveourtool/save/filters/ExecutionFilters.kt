package com.saveourtool.save.filters

import kotlinx.datetime.*
import kotlinx.serialization.Serializable

/**
 * Aff filters in one property
 * @property startTime to filter by [startTime] execution
 * @property endTime to use filter by [endTime] execution
 */
@Serializable
data class ExecutionFilters(

    val startTime: LocalDateTime,

    val endTime: LocalDateTime,

) {
    companion object {
        val empty = ExecutionFilters(
            startTime = LocalDateTime(1970, 1, 1, 0, 0, 0),
            endTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
        )
    }
}
