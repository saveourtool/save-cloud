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
    is String -> principal as String
    is UserDetails -> (principal as UserDetails).username
    else -> error("Authentication instance $this has unsupported type of principal: $principal of type ${principal::class}")
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

/**
 * @return Entity [User] created from provided [Map]
 */
internal fun Map<String, Any>.toUserEntity(): User {
    val record = this
    return User(
        name = record["name"] as String,
        password = record["password"] as String?,
        role = record["role"] as String?,
        email = record["email"] as String?,
        avatar = record["avatar"] as String?,
        company = record["company"] as String?,
        location = record["location"] as String?,
        linkedin = record["linkedin"] as String?,
        gitHub = record["git_hub"] as String?,
        twitter = record["twitter"] as String?,
        status = record["status"] as UserStatus,
        rating = record["rating"] as Long,
    ).apply {
        this.id = record["id"] as Long
    }
}