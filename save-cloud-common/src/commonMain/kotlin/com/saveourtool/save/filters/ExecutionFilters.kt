package com.saveourtool.save.filters

import kotlinx.serialization.Serializable

/**
 * Aff filters in one property
 * @property startTime to filter by [startTime] execution formatting: <year, month, day>
 * @property endTime to use filter by [endTime] execution formatting: <year, month, day>
 */
@Serializable
data class ExecutionFilters(

    val startTime: Triple<Int, Int, Int>,

    val endTime: Triple<Int, Int, Int>?,
) {
    companion object {
        val empty = ExecutionFilters(startTime = Triple(1970, 0, 1), endTime = null)
    }
}
