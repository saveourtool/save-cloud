package com.saveourtool.save.entities.contest

import kotlinx.serialization.Serializable

/**
 * @property contestSample contest sample dto
 * @property name name of field
 * @property type type of field
 */
@Serializable
data class ContestSampleFieldDto(
    val contestSample: ContestSampleDto,
    val name: String,
    val type: ContestSampleFieldType,
) {
    companion object {
        val empty = ContestSampleFieldDto(
            ContestSampleDto.empty,
            "",
            ContestSampleFieldType.NUM,
        )
    }
}
