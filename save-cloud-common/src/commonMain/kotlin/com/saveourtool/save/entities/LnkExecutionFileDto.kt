package com.saveourtool.save.entities

import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.testsuite.TestSuiteDto
import kotlinx.serialization.Serializable

/**
 * @property execution
 * @property file
 */
@Serializable
data class LnkExecutionFileDto(
    val execution: ExecutionDto,
    val file: FileDto,
)
