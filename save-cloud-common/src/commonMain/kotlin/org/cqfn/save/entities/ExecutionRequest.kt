package org.cqfn.save.entities

import org.cqfn.save.domain.Sdk

import kotlinx.serialization.Serializable

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitDto github repository
 * @property propertiesRelativePath location of save.properties file to start the execution, relative to project's root directory
 * @property executionId id of execution. It will change after execution will created. This need to update execution status, if there will be problem with git cloning
 * @property sdk
 */
@Serializable
data class ExecutionRequest(
    val project: Project,
    val gitDto: GitDto,
    val propertiesRelativePath: String = "save.properties",
    val sdk: List<Sdk>,
    var executionId: Long?,
)
