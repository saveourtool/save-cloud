package com.saveourtool.save.filters

import com.saveourtool.save.entities.ProjectStatus
import kotlinx.serialization.Serializable

/**
 * @property name
 * @property status
 */
@Serializable
data class ProjectFilters(
    val name: String,
    val status: List<ProjectStatus> = listOf(ProjectStatus.CREATED),
) {
    companion object {
        val empty = ProjectFilters("")
        val any = ProjectFilters("", ProjectStatus.values().toList())
    }
}
