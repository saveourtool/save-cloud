package com.saveourtool.save.entities

import com.saveourtool.save.utils.LocalDateTime
import com.saveourtool.save.validation.Validatable
import com.saveourtool.save.validation.isValidName
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Data class of contest information
 *
 * @property name contest name
 * @property description info about contest
 * @property startTime start time of a contest
 * @property endTime end time of a contest
 * @property organizationName
 * @property testSuiteIds
 * @property creationTime
 */
@Serializable
data class ContestDto(
    val name: String,
    @Contextual
    val startTime: LocalDateTime?,
    @Contextual
    val endTime: LocalDateTime?,
    val description: String?,
    val organizationName: String,
    val testSuiteIds: List<Long>,
    @Contextual
    val creationTime: LocalDateTime?,
) : Validatable {
    override fun validate(): Boolean = name.isValidName()

    companion object {
        val empty = ContestDto(
            "",
            null,
            null,
            null,
            "",
            emptyList(),
            null,
        )
    }
}
