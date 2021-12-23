package org.cqfn.save.domain.execution

import org.cqfn.save.entities.ProjectDto
import kotlinx.serialization.Serializable

/**
 * Additional information about execution that will be save for already created execution
 *
 * @property project project of new execution
 * @property testSuiteIds test suite ids of new execution
 * @property resourcesRootPath path to resources of new execution
 * @property version version of new execution
 */
@Serializable
data class ExecutionInitializationDto(
    val project: ProjectDto,
    val testSuiteIds: String,
    val resourcesRootPath: String,
    val version: String,
)
