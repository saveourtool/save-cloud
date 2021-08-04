/**
 * Data classes that are used to trigger execution start
 */

package org.cqfn.save.entities

import org.cqfn.save.domain.Sdk

import kotlinx.serialization.Serializable

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitDto data about project's git repository
 * @property propertiesRelativePath location of save.properties file to start the execution, relative to project's root directory
 * @property executionId id of execution. It is null until execution is created (when request comes from frontend).
 * @property sdk
 */
@Serializable
data class ExecutionRequest(
    val project: Project,
    val gitDto: GitDto,
    val propertiesRelativePath: String = "save.properties",
    val sdk: Sdk,
    val executionId: Long?,
)

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
)
