package com.saveourtool.save.info

import kotlinx.serialization.Serializable

/**
 * @property inInOrganizations user permissions in organizations
 */
@Serializable
data class UserPermissions(
    val inInOrganizations: Map<String, UserPermissionsInOrganization> = emptyMap(),
)
