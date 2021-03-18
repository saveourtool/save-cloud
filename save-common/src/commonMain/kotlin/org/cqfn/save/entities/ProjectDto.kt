package org.cqfn.save.entities

import kotlinx.serialization.Serializable
import org.cqfn.save.repository.GitRepository

/**
 * Data class of information about project
 *
 * @property project project
 * @property gitRepository github repository
 */
@Serializable
data class ProjectDto(
    val project: Project,
    val gitRepository: GitRepository,
)
