package com.saveourtool.save.backend.security

import com.saveourtool.save.backend.repository.LnkUserProjectRepository
import com.saveourtool.save.backend.service.LnkUserProjectService
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.User
import com.saveourtool.save.permission.Permission
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

    @Autowired
    private lateinit var lnkUserProjectRepository: LnkUserProjectRepository

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
        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        val userId = (authentication.details as AuthenticationDetails).id
        val projectRole = lnkUserProjectService.findRoleByUserIdAndProject(userId, project)

        return when (permission) {
            Permission.READ -> project.public || projectRole.isHigherOrEqualThan(Role.VIEWER)
            Permission.WRITE -> projectRole.isHigherOrEqualThan(Role.ADMIN)
            Permission.DELETE -> projectRole.isHigherOrEqualThan(Role.OWNER)
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

    /**
     * @param project in which the role is going to be changed
     * @param authentication auth info of a current user
     * @param otherUser user whose role is going to be changed
     * @param requestedRole role that is going to be set
     * @return true if user can change roles in project and false otherwise
     */
    @Suppress("UnsafeCallOnNullableType")
    fun canChangeRoles(
        project: Project,
        authentication: Authentication,
        otherUser: User,
        requestedRole: Role = Role.NONE
    ): Boolean {
        val selfRole = lnkUserProjectService.getGlobalRoleOrProjectRole(authentication, project)
        val otherUserId = otherUser.id!!
        val otherRole = lnkUserProjectRepository.findByUserIdAndProject(otherUserId, project)?.role ?: Role.NONE
        return isProjectAdminOrHigher(selfRole) &&
                hasAnotherUserLessPermissions(selfRole, otherRole) &&
                isRequestedPermissionsCanBeSetByUser(selfRole, requestedRole)
    }

    /**
     * @param selfRole
     * @param otherRole
     * @return true if [otherRole] has less permissions than [selfRole], false otherwise.
     */
    fun hasAnotherUserLessPermissions(selfRole: Role, otherRole: Role): Boolean = selfRole.priority > otherRole.priority

    /**
     * @param selfRole
     * @param requestedRole
     * @return true if [requestedRole] can be set by user with role [selfRole], false otherwise.
     */
    fun isRequestedPermissionsCanBeSetByUser(selfRole: Role, requestedRole: Role): Boolean = selfRole.priority > requestedRole.priority

    /**
     * @param userRole
     * @return true if [userRole] is [Role.ADMIN] or higher, false otherwise.
     */
    fun isProjectAdminOrHigher(userRole: Role): Boolean = userRole.priority >= Role.ADMIN.priority
}
