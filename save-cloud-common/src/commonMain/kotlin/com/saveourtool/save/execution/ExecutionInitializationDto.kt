package com.saveourtool.save.execution

import com.saveourtool.save.entities.Project
import kotlinx.serialization.Serializable

/**
 * Additional information about execution that will be saved for already created execution
 *
 * @property project project of new execution
 * @property testSuiteIds test suite ids of new execution
 * @property execCmd execCmd for SAVE-cli for testing in standard mode
 * @property batchSizeForAnalyzer batchSize for SAVE-cli for testing in standard mode
 */
@Serializable
data class ExecutionInitializationDto(
    val project: Project,
    val testSuiteIds: List<Long>,
    val execCmd: String?,
    val batchSizeForAnalyzer: String?,
)
