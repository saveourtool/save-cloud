package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * @property organizationName name of organization
 * @property projectName timestamp of project in this organization
 */
@Serializable
data class ProjectCoordinates(
    val organizationName: String,

    val projectName: String,
)
