package org.cqfn.save.entities

import org.cqfn.save.domain.Role
import org.cqfn.save.info.UserInfo
import javax.persistence.Entity

/**
 * @property name
 * @property password *in plain text*
 * @property role role of this user
 * @property source where the user identity is coming from, e.g. "github"
 * @property email email of user
 * @property avatar avatar of user
 * @property company
 * @property location
 * @property linkedin
 * @property gitHub
 * @property twitter
 */
@Entity
@Suppress("LongParameterList")
class User(
    var name: String?,
    var password: String?,
    var role: String?,
    var source: String,
    var email: String? = null,
    var avatar: String? = null,
    var company: String? = null,
    var location: String? = null,
    var linkedin: String? = null,
    var gitHub: String? = null,
    var twitter: String? = null,
) : BaseEntity() {
    /**
     * @param projects roles in projects
     * @return [UserInfo] object
     */
    fun toUserInfo(projects: Map<String, Role> = emptyMap(), organizations: Map<String, Role> = emptyMap()) = UserInfo(
        name = name ?: "Undefined",
        source = source,
        projects = projects,
        organizations = organizations,
        email = email,
        avatar = avatar,
        company = company,
        linkedin = linkedin,
        gitHub = gitHub,
        twitter = twitter,
        location = location,
    )
}
