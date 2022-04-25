package org.cqfn.save.backend.security

import org.cqfn.save.backend.service.LnkUserOrganizationService
import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.permission.Permission
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Class that is capable of assessing user's permissions regarding organizations.
 */
@Component
class OrganizationPermissionEvaluator {
    @Autowired
    private lateinit var lnkUserOrganizationService: LnkUserOrganizationService

    /**
     * @param authentication [Authentication] describing an authenticated request
     * @param organization
     * @param permission
     * @return whether user described by [authentication] can have [permission] on [organization]
     */
    fun hasPermission(authentication: Authentication?, organization: Organization, permission: Permission): Boolean {
        authentication ?: return when (permission) {
            Permission.READ -> true
            Permission.WRITE -> false
            Permission.DELETE -> false
        }

        val userId = (authentication.details as AuthenticationDetails).id
        val organizationRole = lnkUserOrganizationService.findRoleByUserIdAndOrganization(userId, organization)

        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        return when (permission) {
            Permission.READ -> true
            Permission.WRITE -> hasWriteAccess(userId, organizationRole)
            Permission.DELETE -> organizationRole == Role.OWNER
        }
    }

    /**
     * @param authentication
     * @param permission
     * @param statusIfForbidden
     * @return a [Mono] containing the organization or `Mono.error` if organization can't or shouldn't be accessed by the current user
     */
    internal fun Mono<Project?>.filterByPermission(
        authentication: Authentication?,
        permission: Permission,
        statusIfForbidden: HttpStatus,
    ) = switchIfEmpty { Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND)) }
        .cast<Organization>()
        .map { actualOrganization ->
            actualOrganization to hasPermission(authentication, actualOrganization, permission)
        }
        .flatMap { (organization, isPermissionGranted) ->
            if (isPermissionGranted) {
                Mono.just(organization)
            } else {
                // current user lacks permissions
                Mono.error(ResponseStatusException(statusIfForbidden))
            }
        }
        .switchIfEmpty {
            // We get here if organization either is not found or shouldn't be visible for current user.
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

    private fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }

    private fun hasWriteAccess(userId: Long?, organizationRole: Role): Boolean =
        userId?.let { organizationRole.priority >= Role.ADMIN.priority } ?: false
}
