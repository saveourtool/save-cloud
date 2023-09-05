package com.saveourtool.save.info

import kotlinx.serialization.Serializable

/**
 * @property inOrganizations user permissions in organizations
 */
@Serializable
data class UserPermissions(
    val inOrganizations: Map<String, UserPermissionsInOrganization> = emptyMap(),
)
