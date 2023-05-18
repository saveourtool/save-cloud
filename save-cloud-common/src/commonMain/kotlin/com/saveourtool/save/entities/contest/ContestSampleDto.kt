package com.saveourtool.save.entities.contest

import kotlinx.serialization.Serializable

/**
 * @property name name of contest sample
 * @property description description of contest sample
 */
@Serializable
data class ContestSampleDto(
    val name: String,
    val description: String?,
) {
    companion object {
        val empty = ContestSampleDto(
            "",
            null,
        )
    }
}
