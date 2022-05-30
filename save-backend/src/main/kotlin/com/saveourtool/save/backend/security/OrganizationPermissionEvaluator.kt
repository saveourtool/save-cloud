package com.saveourtool.save.backend.security

import com.saveourtool.save.backend.service.LnkUserOrganizationService
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.permission.Permission
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

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
        authentication ?: return false
        val userId = (authentication.details as AuthenticationDetails).id
        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        val organizationRole = lnkUserOrganizationService.findRoleByUserIdAndOrganization(userId, organization)
        return when (permission) {
            Permission.READ -> hasReadAccess(userId, organizationRole)
            Permission.WRITE -> hasWriteAccess(userId, organizationRole)
            Permission.DELETE -> hasDeleteAccess(userId, organizationRole)
        }
    }

    /**
     * @param authentication
     * @param organizationName
     * @param requiredRole
     * @return true if user with [authentication] info has [requiredRole] in organization with name [organizationName] or globally
     */
    fun hasGlobalRoleOrOrganizationRole(authentication: Authentication, organizationName: String, requiredRole: Role): Boolean =
            lnkUserOrganizationService.getGlobalRoleOrOrganizationRole(authentication, organizationName).priority >= requiredRole.priority

    private fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }

    private fun hasReadAccess(userId: Long?, organizationRole: Role): Boolean =
            userId?.let { organizationRole.priority >= Role.VIEWER.priority } ?: false

    private fun hasWriteAccess(userId: Long?, organizationRole: Role): Boolean =
            userId?.let { organizationRole.priority >= Role.ADMIN.priority } ?: false

    private fun hasDeleteAccess(userId: Long?, organizationRole: Role): Boolean =
            userId?.let { organizationRole.priority >= Role.OWNER.priority } ?: false

    /**
     * In case we widen number of users that can manage roles in an organization, there is a separate method.
     * Simply delegating now.
     *
     * @param organization in which the role is going to be changed
     * @param authentication auth info of a current user
     * @param otherUser user whose role is going to be changed
     * @param requestedRole role that is going to be set
     * @return whether the user can change roles in organization
     */
    @Suppress("UnsafeCallOnNullableType")
    fun canChangeRoles(
        organization: Organization,
        authentication: Authentication,
        otherUser: User,
        requestedRole: Role = Role.NONE
    ): Boolean {
        val selfRole = lnkUserOrganizationService.getGlobalRoleOrOrganizationRole(authentication, organization)
        val otherRole = lnkUserOrganizationService.findRoleByUserIdAndOrganization(otherUser.id!!, organization)
        return isOrganizationAdminOrHigher(selfRole) && hasAnotherUserLessPermissions(selfRole, otherRole) &&
                isRequestedPermissionsCanBeSetByUser(selfRole, requestedRole)
    }

    /**
     * @param selfRole
     * @param otherRole
     * @return true if user with [selfRole] has more permissions than user with [otherRole], false otherwise.
     */
    fun hasAnotherUserLessPermissions(selfRole: Role, otherRole: Role): Boolean = selfRole.priority > otherRole.priority

    /**
     * @param selfRole
     * @param requestedRole
     * @return true if [selfRole] is higher than [requestedRole], false otherwise
     */
    fun isRequestedPermissionsCanBeSetByUser(selfRole: Role, requestedRole: Role): Boolean = selfRole.priority > requestedRole.priority

    /**
     * @param userRole
     * @return true if [userRole] is [Role.ADMIN] or higher, false otherwise
     */
    fun isOrganizationAdminOrHigher(userRole: Role): Boolean = userRole.priority >= Role.ADMIN.priority
}
