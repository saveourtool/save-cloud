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

    /**
     * @return true if both [organizationName] and [projectName] are empty, false otherwise
     */
    fun consideredEmpty() = organizationName.isEmpty() && projectName.isEmpty()

    companion object {
        val empty = ProjectCoordinates("", "")
    }
}

/**
 * @return [this] if not null, [ProjectCoordinates.empty] otherwise
 */
fun ProjectCoordinates?.orEmpty() = this ?: ProjectCoordinates.empty
