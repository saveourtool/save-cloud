package com.saveourtool.save.entities

import com.saveourtool.save.testsuite.TestSuiteDto
import kotlinx.serialization.Serializable

/**
 * @property contest
 * @property testSuite
 */
@Serializable
data class LnkContestTestSuiteDto(
    val contest: ContestDto,
    val testSuite: TestSuiteDto,
)
