package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * @property organizationName name of organization
 * @property projectName name of project in this organization
 */
@Serializable
data class ProjectCoordinates(
    val organizationName: String,
    val projectName: String,
) {
    /**
     * @return ProjectCoordinates as formatted string "{organizationName}/{projectName}"
     */
    override fun toString() = "$organizationName/$projectName"
}
