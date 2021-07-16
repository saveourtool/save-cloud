package org.cqfn.save.entities

import kotlinx.serialization.Serializable

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitDto github repository
 * @property propertiesRelativePath location of save.properties file to start the execution, relative to project's root directory
 * @property executionId
 */
@Serializable
data class ExecutionRequest(
    val project: Project,
    val gitDto: GitDto,
    val propertiesRelativePath: String = "save.properties",
    var executionId: Long?,
)
