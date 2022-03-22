package org.cqfn.save.backend.security

import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.springframework.beans.factory.annotation.Autowired
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
    @Autowired
    private lateinit var lnkUserProjectService: LnkUserProjectService

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

        val userId = (authentication.details as AuthenticationDetails).id
        val isOrganizationOwner = project.organization.ownerId == userId
        val projectRole by lazy { lnkUserProjectService.findRoleByUserIdAndProject(userId, project) }

        if (isOrganizationOwner || authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        return when (permission) {
            Permission.READ -> project.public || hasWriteAccess(userId, project, projectRole)
            Permission.WRITE -> hasWriteAccess(userId, project, projectRole)
            Permission.DELETE -> project.userId == userId || projectRole == Role.OWNER
        }
    }

    /**
     * @param authentication
     * @param permission
     * @param statusIfForbidden
     * @return a [Mono] containing the project or `Mono.error` if project can't or shouldn't be accessed by the current user
     */
    internal fun Mono<Project?>.filterByPermission(
        authentication: Authentication?,
        permission: Permission,
        statusIfForbidden: HttpStatus,
    ) = switchIfEmpty { Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND)) }
        .cast<Project>()
        .map { actualProject ->
            actualProject to hasPermission(authentication, actualProject, permission)
        }
        .filter { (project, isPermissionGranted) -> project.public || isPermissionGranted }
        .flatMap { (project, isPermissionGranted) ->
            if (isPermissionGranted) {
                Mono.just(project)
            } else {
                // project is public, but current user lacks permissions
                Mono.error(ResponseStatusException(statusIfForbidden))
            }
        }
        .switchIfEmpty {
            // We get here if `!project.public && !isPermissionGranted`, i.e.
            // if project either is not found or shouldn't be visible for current user.
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

    private fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }

    private fun hasWriteAccess(userId: Long?, project: Project, projectRole: Role): Boolean = when {
        userId == null -> false
        project.userId == userId -> true
        else -> projectRole == Role.ADMIN || projectRole == Role.OWNER
    }

    /**
     * @param authentication
     * @param execution
     * @param permission
     * @return [Mono] containing `true` if the current user is granted [permission] on the project for this [execution] or Mono with `false` otherwise
     */
    internal fun checkPermissions(authentication: Authentication, execution: Execution, permission: Permission): Mono<Boolean> =
            Mono.justOrEmpty(execution.project)
                .filterByPermission(authentication, permission, HttpStatus.FORBIDDEN)
                .map { true }
                .defaultIfEmpty(false)
}
