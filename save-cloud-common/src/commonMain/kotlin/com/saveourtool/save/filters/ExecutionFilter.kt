package com.saveourtool.save.filters

import com.saveourtool.save.utils.getCurrentLocalDateTime
import kotlinx.datetime.*
import kotlinx.serialization.Serializable

/**
 * Aff filters in one property
 * @property startTime to filter by [startTime] execution
 * @property endTime to use filter by [endTime] execution
 */
@Serializable
data class ExecutionFilter(

    val startTime: LocalDateTime,

    val endTime: LocalDateTime,

) {
    companion object {
        val empty = ExecutionFilter(
            startTime = LocalDateTime(1970, 1, 1, 0, 0, 0),
            endTime = getCurrentLocalDateTime()
        )
    }
}
