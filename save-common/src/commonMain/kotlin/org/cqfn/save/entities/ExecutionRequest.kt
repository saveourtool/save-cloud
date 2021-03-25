package org.cqfn.save.entities

import org.cqfn.save.repository.GitRepository

import kotlinx.serialization.Serializable

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitRepository github repository
 */
@Serializable
data class ExecutionRequest(
    val project: Project,
    val gitRepository: GitRepository,
)
