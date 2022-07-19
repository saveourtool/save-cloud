/**
 * Data classes that are used to trigger execution start
 */

package com.saveourtool.save.entities

import com.saveourtool.save.domain.Sdk

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
 * @property sdk
 * @property executionId id of execution. It is null until execution is created (when request comes from frontend).
 */
@Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT")
@Serializable
data class ExecutionRequest(
    override val project: Project,
    val gitDto: GitDto,
    val testRootPath: String,
    override val sdk: Sdk,
    val executionId: Long?,
) : ExecutionRequestBase()

/**
 * @property project
 * @property testSuites
 * @property sdk
 * @property execCmd
 * @property batchSizeForAnalyzer
 * @property executionId
 * @property version
 */
@Serializable
data class ExecutionRequestForStandardSuites(
    override val project: Project,
    val testSuites: List<String>,
    override val sdk: Sdk,
    val execCmd: String?,
    val batchSizeForAnalyzer: String?,
    val executionId: Long?,
    val version: String?,
) : ExecutionRequestBase()

/**
 * @property project
 * @property contestName
 * @property sdk
 * @property execCmd
 * @property batchSizeForAnalyzer
 * @property executionId
 * @property version
 */
@Serializable
data class ExecutionRequestForContest(
    override val project: Project,
    val contestName: String,
    override val sdk: Sdk,
    val execCmd: String?,
    val batchSizeForAnalyzer: String?,
    val executionId: Long?,
    val version: String?,
) : ExecutionRequestBase()
