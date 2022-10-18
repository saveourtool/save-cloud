/**
 * Authentication utilities
 */
// TODO MOVE METHODS FROM COMMON HERE
package com.saveourtool.save.authservice.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.utils.AuthenticationDetails
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.access.hierarchicalroles.RoleHierarchyUtils
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication

/**
 * @return userId
 */
fun Authentication.userId() = (this.details as AuthenticationDetails).id

/**
 * @return username
 */
fun Authentication.userName() = this.extractUserNameAndIdentitySource().first

/**
 * @return identitySource
 */
fun Authentication.identitySource() = this.extractUserNameAndIdentitySource().second

/**
 * @return pair of username and identitySource
 * @throws BadCredentialsException
 */
fun Authentication.extractUserNameAndIdentitySource(): Pair<String, String> {
    val identitySource = (this.details as AuthenticationDetails).identitySource
    if (identitySource == null || !this.name.startsWith("$identitySource:")) {
        throw BadCredentialsException(this.name)
    }
    val name = this.name.drop(identitySource.length + 1)
    return name to identitySource
}

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
