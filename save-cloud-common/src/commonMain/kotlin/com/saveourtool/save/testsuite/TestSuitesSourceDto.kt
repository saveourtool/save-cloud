package com.saveourtool.save.testsuite

import kotlinx.serialization.Serializable

/**
 * @property type
 * @property name
 * @property description
 * @property locationType
 * @property locationInfo
 */
@Serializable
data class TestSuitesSourceDto(
    val type: TestSuiteType,
    val name: String,
    val description: String?,
    val locationType: TestSuitesSourceLocationType,
    val locationInfo: String,
)
