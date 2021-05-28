package org.cqfn.save.entities

import org.cqfn.save.repository.GitRepository

import kotlinx.serialization.Serializable

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitRepository github repository
 * @property propertiesRelativePath location of save.properties file to start the execution, relative to project's root directory
 */
@Serializable
data class ExecutionRequest(
    val project: Project,
    val gitRepository: GitRepository,
    val propertiesRelativePath: String = "save.properties",
)
