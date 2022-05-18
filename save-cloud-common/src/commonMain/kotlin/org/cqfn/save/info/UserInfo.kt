package org.cqfn.save.info

import org.cqfn.save.domain.Role

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
 */
@Serializable
data class UserInfo(
    val name: String,
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
)
