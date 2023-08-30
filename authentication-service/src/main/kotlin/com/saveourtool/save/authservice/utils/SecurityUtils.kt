/**
 * Authentication utilities
 */

package com.saveourtool.save.authservice.utils

import com.saveourtool.save.domain.Role
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.access.hierarchicalroles.RoleHierarchyUtils
import org.springframework.security.core.Authentication

/**
 * Extract userId from this [Authentication]
 * We assume that the authentication uses [SaveUserDetails] as principal
 *
 * @return userId
 */
fun Authentication.userId() = (principal as SaveUserDetails).id

/**
 * Extract username from this [Authentication].
 * We assume that the authentication uses [SaveUserDetails] as principal
 *
 * @return username
 */
fun Authentication.username(): String = (principal as SaveUserDetails).name

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
