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

    /**
     * Execution id of request, it's null on first request
     */
    abstract val executionId: Long?

    /**
     * null in [ExecutionRequest]
     */
    abstract val execCmd: String?

    /**
     * null in [ExecutionRequest]
     */
    abstract val batchSizeForAnalyzer: String?
}

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitDto data about project's git repository
 * @property branchOrCommit
 * @property testRootPath root path of the test repository where save.properties file and high level save.toml file could be stored
 * @property sdk
 * @property executionId id of execution. It is null until execution is created (when request comes from frontend).
 * @property execCmd
 * @property batchSizeForAnalyzer
 */
@Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT")
@Serializable
data class ExecutionRequest(
    override val project: Project,
    val gitDto: GitDto,
    val branchOrCommit: String?,
    val testRootPath: String,
    override val sdk: Sdk,
    override val executionId: Long?,
    override val execCmd: String? = null,
    override val batchSizeForAnalyzer: String? = null,
) : ExecutionRequestBase()

/**
 * @property project
 * @property testSuites
 * @property sdk
 * @property execCmd
 * @property batchSizeForAnalyzer
 * @property executionId
 */
@Serializable
data class ExecutionRequestForStandardSuites(
    override val project: Project,
    val testSuites: List<String>,
    override val sdk: Sdk,
    override val execCmd: String?,
    override val batchSizeForAnalyzer: String?,
    override val executionId: Long?,
) : ExecutionRequestBase()
