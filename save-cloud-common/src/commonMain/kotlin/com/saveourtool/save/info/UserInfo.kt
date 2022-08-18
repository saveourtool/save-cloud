package com.saveourtool.save.info

import com.saveourtool.save.domain.Role

import kotlinx.serialization.Serializable

/**
 * Represents all data related to the User
 *
 * @property name name/login of the user
 * @property source where the user identity is coming from, e.g. "github"
 * @property projects [String] of project name to [Role] of the user
 * @property email
 * @property avatar avatar of user
 * @property company
 * @property location
 * @property linkedin
 * @property gitHub
 * @property twitter
 * @property organizations
 * @property globalRole
 * @property id
 * @property oldNames
 * @property isActive
 */
@Serializable
data class UserInfo(
    val name: String,
    val id: Long? = null,
    val oldName: String? = null,
    val originalLogins: List<String?> = emptyList(),
    val source: String? = null,
    val projects: Map<String, Role> = emptyMap(),
    val organizations: Map<String, Role> = emptyMap(),
    val email: String? = null,
    val avatar: String? = null,
    var company: String? = null,
    var location: String? = null,
    var linkedin: String? = null,
    var gitHub: String? = null,
    var twitter: String? = null,
    val globalRole: Role? = null,
    var isActive: Boolean = false,
)
