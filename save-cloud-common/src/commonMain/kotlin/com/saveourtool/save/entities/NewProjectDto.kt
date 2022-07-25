package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * Data class of new project information
 *
 * @property project project
 * @property organizationName
 */
@Serializable
data class NewProjectDto(
    val project: Project,
    val organizationName: String,
)
