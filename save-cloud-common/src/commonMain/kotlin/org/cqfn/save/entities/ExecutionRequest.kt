/**
 * Data classes that are used to trigger execution start
 */

package org.cqfn.save.entities

import org.cqfn.save.domain.Sdk

import kotlinx.serialization.Serializable

/**
 * Base class for execution requests
 */
sealed class ExecutionRequestBase {
    /**
     * a [Project] for which execution is being requested
     */
    abstract val project: Project

    /**
     * An SDK for this execution
     */
    abstract val sdk: Sdk
}

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitDto data about project's git repository
 * @property testRootPath root path of the test repository where save.properties file and high level save.toml file could be stored
 * @property executionId id of execution. It is null until execution is created (when request comes from frontend).
 * @property sdk
 */
@Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT")
@Serializable
data class ExecutionRequest(
    override val project: Project,
    val gitDto: GitDto,
    // empty testRootPath is not valid and user needs to pass "." instead. Let's make it as a default by for now.
    val testRootPath: String = ".",
    override val sdk: Sdk,
    val executionId: Long?,
) : ExecutionRequestBase()

/**
 * @property project
 * @property testsSuites
 * @property sdk
 */
@Serializable
data class ExecutionRequestForStandardSuites(
    override val project: Project,
    val testsSuites: List<String>,
    override val sdk: Sdk,
) : ExecutionRequestBase()
