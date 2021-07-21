package org.cqfn.save.entities

import kotlinx.serialization.Serializable
import org.cqfn.save.domain.Sdk

/**
 * @property project
 * @property testsSuites
 */
@Serializable
data class ExecutionRequestForStandardSuites(
    val project: Project,
    val testsSuites: List<String>,
    val sdk: List<Sdk>,
)
