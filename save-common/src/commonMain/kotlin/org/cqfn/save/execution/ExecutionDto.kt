package org.cqfn.save.execution

import org.cqfn.save.entities.Project

import kotlinx.serialization.Serializable

/**
 * @property project
 * @property startTime
 * @property endTime
 * @property status
 * @property testSuiteIds
 * @property resourcesRootPath
 */
@Serializable
data class ExecutionDto(
    val project: Project,
    var startTime: Long,
    var endTime: Long,
    var status: ExecutionStatus,
    var testSuiteIds: List<String>,
    var resourcesRootPath: String,
)
