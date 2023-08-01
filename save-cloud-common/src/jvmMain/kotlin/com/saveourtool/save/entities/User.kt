package com.saveourtool.save.entities

import com.saveourtool.save.domain.Role
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.info.UserStatus
import com.saveourtool.save.spring.entity.BaseEntity

import com.fasterxml.jackson.annotation.JsonIgnore

import javax.persistence.*

/**
 * @property name
 * @property password *in plain text*
 * @property role role of this user
 * @property email email of user
 * @property avatar avatar of user
 * @property company
 * @property location
 * @property linkedin
 * @property gitHub
 * @property twitter
 * @property status
 * @property originalLogins
 * @property rating rating of user
 */
@Entity
@Suppress("LongParameterList")
class User(
    var name: String,
    var password: String?,
    var role: String?,
    var email: String? = null,
    var avatar: String? = null,
    var company: String? = null,
    var location: String? = null,
    var linkedin: String? = null,
    var gitHub: String? = null,
    var twitter: String? = null,
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.CREATED,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "user",
        targetEntity = OriginalLogin::class
    )
    @JsonIgnore
    var originalLogins: List<OriginalLogin> = emptyList(),
    var rating: Long = 0,
    var website: String? = null,
    var freeText: String? = null,
    var realName: String? = null,
    ) : BaseEntity() {
    /**
     * @param projects roles in projects
     * @param organizations roles in organizations
     * @return [UserInfo] object
     */
    fun toUserInfo(projects: Map<String, Role> = emptyMap(), organizations: Map<String, Role> = emptyMap()) = UserInfo(
        id = id,
        name = name,
        originalLogins = originalLogins.map { it.name },
        projects = projects,
        organizations = organizations,
        email = email,
        avatar = avatar,
        company = company,
        linkedin = linkedin,
        gitHub = gitHub,
        twitter = twitter,
        location = location,
        status = status,
        rating = rating,
        website = website,
        freeText = freeText,
        realName = realName,
    )
}
