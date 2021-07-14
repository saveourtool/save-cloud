package org.cqfn.save.execution

import org.cqfn.save.entities.Project
import kotlinx.serialization.Serializable

@Serializable
data class ExecutionUpdateCreationDto(
    val project: Project,
    val testSuiteIds: String,
    val resourcesRootPath: String,
    val batchSize: Int,
    val version: String,
)
