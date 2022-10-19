package com.saveourtool.save.filters

import kotlinx.serialization.Serializable

/**
 * Aff filters in one property
 * @property startTime to filter by [startTime] execution
 * @property endTime to use filter by [endTime] execution
 */
@Serializable
data class ExecutionFilters(

    val startTime: Long?,

    val endTime: Long?,
) {
    companion object {
        val empty = ExecutionFilters(endTime = null, startTime = null)
    }
}
