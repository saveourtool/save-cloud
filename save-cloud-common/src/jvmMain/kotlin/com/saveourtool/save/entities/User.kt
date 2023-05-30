package com.saveourtool.save.entities

import com.saveourtool.save.domain.Role
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.spring.entity.BaseEntity

import com.fasterxml.jackson.annotation.JsonIgnore

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.OneToMany

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
 * @property isActive
 * @property originalLogins
 * @property rating rating of user
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
    var isActive: Boolean = false,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "user",
        targetEntity = OriginalLogin::class
    )
    @JsonIgnore
    var originalLogins: List<OriginalLogin> = emptyList(),
    var rating: Long = 0,
) : BaseEntity() {
    /**
     * @param projects roles in projects
     * @param organizations roles in organizations
     * @return [UserInfo] object
     */
    fun toUserInfo(projects: Map<String, Role> = emptyMap(), organizations: Map<String, Role> = emptyMap()) = UserInfo(
        id = id,
        name = name ?: "Undefined",
        originalLogins = originalLogins.map { it.name },
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
        isActive = isActive,
    )
}
