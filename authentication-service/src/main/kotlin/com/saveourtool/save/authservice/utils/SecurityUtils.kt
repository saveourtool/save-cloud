/**
 * Authentication utilities
 */

package com.saveourtool.save.authservice.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.info.UserStatus
import org.springframework.http.HttpHeaders
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.access.hierarchicalroles.RoleHierarchyUtils
import org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authorization.AuthorizationContext
import reactor.core.publisher.Mono

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
 * Extract status from this [Authentication].
 * We assume that the authentication uses [SaveUserDetails] as principal
 *
 * @return status as [String]
 */
fun Authentication.status(): String = (principal as SaveUserDetails).status

/**
 * Extract status from this [Authentication] and check if it is [UserStatus.ACTIVE].
 * We assume that the authentication uses [SaveUserDetails] as principal
 *
 * @return true if [status] is [UserStatus.ACTIVE], false otherwise
 */
fun Authentication.isActive(): Boolean = status() == UserStatus.ACTIVE.toString()

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
 * Get default [AuthorizationDecision] by [authentication] and [authorizationContext]
 *
 * @param authentication
 * @param authorizationContext
 * @return [Mono] of [AuthorizationDecision]
 */
fun defaultAuthorizationDecision(
    authentication: Mono<Authentication>,
    authorizationContext: AuthorizationContext,
): Mono<AuthorizationDecision> = AuthenticatedReactiveAuthorizationManager.authenticated<AuthorizationContext>().check(
    authentication, authorizationContext
).map {
    if (!it.isGranted) {
        // if request is not authorized by configured authorization manager, then we allow only requests w/o Authorization header
        // then backend will return 401, if endpoint is protected for anonymous access
        val hasAuthorizationHeader = authorizationContext.exchange.request.headers[HttpHeaders.AUTHORIZATION].isNullOrEmpty()
        AuthorizationDecision(hasAuthorizationHeader)
    } else {
        it
    }
}
