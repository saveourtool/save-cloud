package com.saveourtool.save.filters

import com.saveourtool.save.entities.ProjectStatus
import kotlinx.serialization.Serializable

/**
 * @property name
 */
@Serializable
data class ProjectFilters(
    val name: String?,
    val status: ProjectStatus = ProjectStatus.CREATED,
) {
    companion object {
        val empty = ProjectFilters("", ProjectStatus.CREATED)
    }
}
