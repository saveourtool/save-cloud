package org.cqfn.save.entities

import org.cqfn.save.utils.EnumType

import kotlinx.serialization.Serializable

/**
 * @property owner
 * @property name
 * @property url
 * @property description description of the project, may be absent
 * @property status status of project
 * @property public
 */
@Serializable
data class ProjectDto(
    val owner: String,
    val name: String,
    val url: String?,
    val description: String?,
    val status: ProjectStatus,
    val public: Boolean = true,
)
