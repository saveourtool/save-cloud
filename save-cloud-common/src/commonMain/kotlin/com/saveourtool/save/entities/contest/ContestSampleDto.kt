package com.saveourtool.save.entities.contest

import kotlinx.serialization.Serializable

/**
 * @property name name of contest sample
 * @property description description of contest sample
 * @property fields list of fields
 */
@Serializable
data class ContestSampleDto(
    val name: String,
    val description: String?,
    val fields: List<ContestSampleFieldDto> = emptyList(),
) {
    companion object {
        val empty = ContestSampleDto(
            "",
            null,
        )
    }
}
