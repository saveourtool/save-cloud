package org.cqfn.save.entities

import org.cqfn.save.domain.Sdk

import kotlinx.serialization.Serializable

sealed class ExecutionRequestBase

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitDto data about project's git repository
 * @property propertiesRelativePath location of save.properties file to start the execution, relative to project's root directory
 * @property executionId id of execution. It will change after execution is created. This need to update execution status, if there will be problem with git cloning
 * @property sdk
 */
@Serializable
data class ExecutionRequest(
    val project: Project,
    val gitDto: GitDto,
    val propertiesRelativePath: String = "save.properties",
    val sdk: Sdk,
    val executionId: Long?,
) : ExecutionRequestBase()

/**
 * @property project
 * @property testsSuites
 * @property sdk
 */
@Serializable
data class ExecutionRequestForStandardSuites(
    val project: Project,
    val testsSuites: List<String>,
    val sdk: Sdk,
) : ExecutionRequestBase()

/**
 * @property executionId
 * @property gitDto
 */
@Serializable
data class ExecutionRerunRequest(
    val executionId: Long,
    val gitDto: GitDto,
) : ExecutionRequestBase()
