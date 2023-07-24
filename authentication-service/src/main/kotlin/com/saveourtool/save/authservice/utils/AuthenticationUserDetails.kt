package com.saveourtool.save.authservice.utils

import com.saveourtool.save.entities.User
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails

/**
 * @property id [com.saveourtool.save.entities.User.id]
 * @property name [com.saveourtool.save.entities.User.name]
 * @property role [com.saveourtool.save.entities.User.role]
 */
data class AuthenticationUserDetails(
    val id: Long,
    val name: String,
    val role: String,
) {
    constructor(user: User) : this(user.requiredId(), user.name, user.role.orEmpty())

    /**
     * @return Spring's [UserDetails] created from save's [User]
     */
    fun toSpringUserDetails(): UserDetails = org.springframework.security.core.userdetails.User.withUsername(name)
        .password("")
        .authorities(AuthorityUtils.commaSeparatedStringToAuthorityList(role))
        .build()
}
