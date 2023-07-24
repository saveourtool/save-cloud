package com.saveourtool.save.entities.contest

import com.saveourtool.save.testsuite.TestSuiteVersioned
import com.saveourtool.save.validation.Validatable
import com.saveourtool.save.validation.isValidName

import kotlin.js.JsExport
import kotlinx.datetime.LocalDateTime
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
 * @property testSuites
 * @property creationTime
 * @property status
 */
@Serializable
@JsExport
data class ContestDto(
    val name: String,
    val status: ContestStatus,
    @Contextual
    val startTime: LocalDateTime?,
    @Contextual
    val endTime: LocalDateTime?,
    val description: String?,
    val organizationName: String,
    val testSuites: List<TestSuiteVersioned>,
    @Contextual
    val creationTime: LocalDateTime?,
) : Validatable {
    override fun validate(): Boolean = name.isValidName()

    companion object {
        val empty = ContestDto(
            "",
            ContestStatus.CREATED,
            null,
            null,
            null,
            "",
            emptyList(),
            null,
        )
    }
}
