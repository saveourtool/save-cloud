package org.cqfn.save.info

import kotlinx.serialization.Serializable
import org.cqfn.save.domain.Role

/**
 * Represents all data related to the Organization
 *
 * @property name organization name
 * @property avatar avatar of organization
 */
@Serializable
data class OrganizationInfo(
    val name: String,
    val userRoles: Map<String, Role> = emptyMap(),
    val avatar: String? = null,
)
