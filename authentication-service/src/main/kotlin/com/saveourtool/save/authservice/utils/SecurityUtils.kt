/**
 * Authentication utilities
 */

package com.saveourtool.save.authservice.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserStatus
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.access.hierarchicalroles.RoleHierarchyUtils
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails

/**
 * Extract userId from this [Authentication]
 *
 * @return userId
 */
fun Authentication.userId() = (this.details as AuthenticationDetails).id

/**
 * Extract username from this [Authentication].
 * We assume here that most of the authentications are created by [ConvertingAuthenticationManager],
 * so `principal` is a String, containing identity source.
 *
 * @return username
 */
fun Authentication.username(): String = when (principal) {
    // this should be the most common branch, as requests are authenticated by `ConvertingAuthenticationManager`
    is String -> (principal as String).split(':').last()
    is UserDetails -> (principal as UserDetails).username
    else -> error("Authentication instance $this has unsupported type of principal: $principal of type ${principal::class}")
}

/**
 * Extract identitySource from this [Authentication].
 *
 * @return identitySource
 * @throws BadCredentialsException
 */
fun Authentication.identitySource(): String? = (this.details as AuthenticationDetails).identitySource

/**
 * @return pair of username and identitySource from this [Authentication].
 * @throws BadCredentialsException
 */
fun Authentication.extractUserNameAndIdentitySource(): Pair<String, String> = this.username() to run {
    val identitySource = this.identitySource()
    if (identitySource == null || !this.name.startsWith("$identitySource:")) {
        throw BadCredentialsException(this.name)
    }
    identitySource
}

/**
 * Convert [Authentication] to [User] based on convention in backend.
 * We assume here that all authentications are created by [ConvertingAuthenticationManager],
 * so `principal` is a String, containing identity source.
 *
 * @return [User]
 */
fun Authentication.toUser(): User {
    val (identitySource, name) = (principal as String).split(':')
    return User(
        name = name,
        password = null,
        email = null,
        role = (this as UsernamePasswordAuthenticationToken).authorities.joinToString(separator = ","),
        source = identitySource,
        status = UserStatus.CREATED,
    )
}

/**
 * Set role hierarchy for spring security
 *
 * @return map of role hierarchy
 */
fun roleHierarchy(): RoleHierarchy = mapOf(
    Role.SUPER_ADMIN to listOf(Role.ADMIN, Role.OWNER, Role.VIEWER),
    Role.ADMIN to listOf(Role.OWNER, Role.VIEWER),
    Role.OWNER to listOf(Role.VIEWER),
)
    .mapKeys { it.key.asSpringSecurityRole() }
    .mapValues { (_, roles) -> roles.map { it.asSpringSecurityRole() } }
    .let(RoleHierarchyUtils::roleHierarchyFromMap)
    .let {
        RoleHierarchyImpl().apply { setHierarchy(it) }
    }
