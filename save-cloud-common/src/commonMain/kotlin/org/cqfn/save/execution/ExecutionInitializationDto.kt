package org.cqfn.save.execution

import org.cqfn.save.entities.Project
import kotlinx.serialization.Serializable

/**
 * Additional information about execution that will be save for already created execution
 *
 * @property project project of new execution
 * @property testSuiteIds test suite ids of new execution
 * @property resourcesRootPath path to resources of new execution
 * @property version version of new execution
 * @property execCmd execution command of new execution, applicable only in standard mode
 * @property batchSizeForAnalyzer batch size of new execution, applicable only in standard mode
 */
@Serializable
data class ExecutionInitializationDto(
    val project: Project,
    val testSuiteIds: String,
    val resourcesRootPath: String,
    val version: String,
    val execCmd: String,
    val batchSizeForAnalyzer: String,
)
