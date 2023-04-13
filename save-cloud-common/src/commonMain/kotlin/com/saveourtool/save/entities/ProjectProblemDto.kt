package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * @property name
 * @property description
 * @property critical
 * @property vulnerabilityName
 */
@Serializable
data class ProjectProblemDto(
    val name: String,
    val description: String,
    val critical: ProjectProblemCritical,
    val vulnerabilityName: String?,
) {
    companion object {
        val empty = ProjectProblemDto(
            "",
            "",
            ProjectProblemCritical.LOW,
            null,
        )
    }
}
