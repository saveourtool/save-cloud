package org.cqfn.save.backend.security

import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Class that is capable of assessing user's permissions regarding projects.
 */
@Component
class ProjectPermissionEvaluator {
    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param project
     * @param permission
     * @return whether user described by [authentication] can have [permission] on project [project]
     */
    fun hasPermission(authentication: Authentication?, project: Project, permission: Permission): Boolean {
        authentication ?: return when (permission) {
            Permission.READ -> project.public
            Permission.WRITE -> false
            Permission.DELETE -> false
        }

        if (authentication.hasRole(Role.ADMIN)) {
            return true
        }

        val userId = (authentication.details as AuthenticationDetails).id
        return when (permission) {
            Permission.READ -> project.public || hasWriteAccess(userId, project)
            Permission.WRITE -> hasWriteAccess(userId, project)
            Permission.DELETE -> project.userId == userId
        }
    }

    internal fun Mono<Project?>.filterByPermission(
        authentication: Authentication?,
        permission: Permission,
        statusIfForbidden: HttpStatus,
    ) = switchIfEmpty { Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND)) }
        .cast<Project>()
        .filter { actualProject ->
            hasPermission(authentication, actualProject, permission)
        }
        .switchIfEmpty { Mono.error(ResponseStatusException(statusIfForbidden)) }

    private fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }

    private fun hasWriteAccess(userId: Long?, project: Project) =
            userId != null && (project.userId == userId || userId in project.adminIdList())
}
