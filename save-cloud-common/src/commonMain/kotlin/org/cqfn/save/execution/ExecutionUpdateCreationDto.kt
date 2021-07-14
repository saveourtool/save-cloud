package org.cqfn.save.execution

import org.cqfn.save.entities.Project
import kotlinx.serialization.Serializable

/**
 * @property project
 * @property testSuiteIds
 * @property resourcesRootPath
 * @property batchSize
 * @property version
 */
@Serializable
data class ExecutionUpdateCreationDto(
    val project: Project,
    val testSuiteIds: String,
    val resourcesRootPath: String,
    val batchSize: Int,
    val version: String,
)
