package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * @property name
 * @property description
 * @property critical
 */
@Serializable
data class ProjectProblemDto(
    val name: String,
    val description: String,
    val critical: ProjectProblemCritical,
)
