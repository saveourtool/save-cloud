package org.cqfn.save.entities

import kotlinx.serialization.Serializable

/**
 * @property project
 * @property testsSuites
 */
@Serializable
data class ExecutionRequestForStandardSuites(
    val project: Project,
    val testsSuites: List<String>,
)
