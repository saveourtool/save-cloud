package com.saveourtool.save.info

import com.saveourtool.save.domain.Role

import kotlinx.serialization.Serializable

/**
 * Represents all data related to the Organization
 *
 * @property name organization name
 * @property userRoles map that matches usernames and roles
 * @property avatar avatar of organization
 */
@Serializable
data class OrganizationInfo(
    val name: String,
    val userRoles: Map<String, Role> = emptyMap(),
    val avatar: String? = null,
)
