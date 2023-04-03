package com.saveourtool.save.entities

import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.testsuite.TestSuiteDto
import kotlinx.serialization.Serializable

/**
 * @property execution
 * @property testSuite
 */
@Serializable
data class LnkExecutionTestSuiteDto(
    val execution: ExecutionDto,
    val testSuite: TestSuiteDto,
)
