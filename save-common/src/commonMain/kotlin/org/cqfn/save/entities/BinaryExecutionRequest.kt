package org.cqfn.save.entities

import kotlinx.serialization.Serializable

/**
 * @property project
 * @property testsSuites
 */
@Serializable
data class BinaryExecutionRequest(
    val project: Project,
    val testsSuites: List<String>,
)
