package org.cqfn.save.entities

import org.cqfn.save.domain.Sdk

import kotlinx.serialization.Serializable

/**
 * @property project
 * @property testsSuites
 * @property sdk
 */
@Serializable
data class ExecutionRequestForStandardSuites(
    val project: Project,
    val testsSuites: List<String>,
    val sdk: List<Sdk>,
)
