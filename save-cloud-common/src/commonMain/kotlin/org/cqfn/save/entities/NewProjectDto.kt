package org.cqfn.save.entities

import kotlinx.serialization.Serializable

/**
 * Data class of new project information
 *
 * @property project project
 * @property gitDto info about git repository
 */
@Serializable
data class NewProjectDto(
    val project: Project,
    val gitDto: GitDto?
)
