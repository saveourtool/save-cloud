package com.saveourtool.save.testsuite

import kotlinx.serialization.Serializable

/**
 * @property name [com.saveourtool.save.entities.TestSuite.name]
 * @property description [com.saveourtool.save.entities.TestSuite.description]
 * @property source [com.saveourtool.save.entities.TestSuite.source]
 * @property version [com.saveourtool.save.entities.TestSuite.testRootPath]
 * @property language [com.saveourtool.save.entities.TestSuite.language]
 * @property tags [com.saveourtool.save.entities.TestSuite.tags]
 */
@Serializable
data class TestSuiteDto(
    val name: String,
    val description: String?,
    val source: TestSuitesSourceDto,
    val version: String,
    val language: String? = null,
    val tags: List<String>? = null,
)
