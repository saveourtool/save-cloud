package com.saveourtool.save.cosv.service

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.cosv.repositorysave.OrganizationRepository
import com.saveourtool.save.cosv.utils.hasRole
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.info.UserPermissions
import com.saveourtool.save.info.UserPermissionsInOrganization
import com.saveourtool.save.permission.Permission
import org.jetbrains.annotations.Blocking
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

/**
 * Service for organization
 */
@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
) {
    /**
     * @param organization organization for update
     * @return updated organization
     */
    fun saveOrganization(organization: Organization) = organizationRepository.updateOrganization(organization.name, organization.rating)

    /**
     * @param name
     * @return organization with [name]
     */
    fun getOrganizationByName(name: String): Organization = organizationRepository.getOrganizationByName(name)

    /**
     * @param authentication
     * @param organizationName name of organization
     * @param permission
     * @return true if [authentication] has [permission] in [organizationName], otherwise -- false
     */
    @Blocking
    fun hasPermissionInOrganization(
        authentication: Authentication?,
        organizationName: String,
        permission: Permission,
    ): Boolean {
        authentication ?: return permission == Permission.READ
        val userId = authentication.userId()
        if (authentication.hasRole(Role.SUPER_ADMIN)) {
            return true
        }

        val organization = organizationRepository.getOrganizationByName(organizationName)
        val organizationRole = organization.id?.let { organizationRepository.findRoleByUserIdAndOrganization(userId, it) }
        organizationRole ?: return permission == Permission.READ
        return when (permission) {
            Permission.READ -> hasReadAccess(userId, organizationRole)
            Permission.WRITE -> hasWriteAccess(userId, organizationRole)
            Permission.DELETE -> hasDeleteAccess(userId, organizationRole)
            Permission.BAN -> hasBanAccess(userId, organizationRole)
        }
    }

    /**
     * @param authentication
     * @param organizationName
     * @return UserPermissions
     */
    fun getUserPermissionsByOrganizationName(
        authentication: Authentication,
        organizationName: String,
    ): UserPermissions {
        val lnkOrganization = organizationRepository.findByUserNameAndOrganizationStatusAndOrganizationName(authentication.userId(), OrganizationStatus.CREATED.name,
            organizationName)

        val isPermittedCreateContest = lnkOrganization?.organization?.canCreateContests ?: false
        val isPermittedToBulkUpload = lnkOrganization?.organization?.canBulkUpload ?: false

        return UserPermissions(
            mapOf(organizationName to UserPermissionsInOrganization(isPermittedCreateContest, isPermittedToBulkUpload)),
        )
    }

    @Suppress("FunctionOnlyReturningConstant", "UNUSED_PARAMETER")
    private fun hasReadAccess(userId: Long?, organizationRole: Role): Boolean = true

    private fun hasWriteAccess(userId: Long?, organizationRole: Role): Boolean = hasDeleteAccess(userId, organizationRole) ||
            userId?.let { organizationRole == Role.ADMIN } ?: false

    private fun hasDeleteAccess(userId: Long?, organizationRole: Role): Boolean =
            hasBanAccess(userId, organizationRole) || userId?.let { organizationRole == Role.OWNER } ?: false

    /**
     * Only [Role.SUPER_ADMIN] can ban the project. And a user with such a global role has permissions for all actions.
     * Since we have all the rights issued depending on the following, you need to set [false] here
     */
    @Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
    private fun hasBanAccess(userId: Long?, organizationRole: Role): Boolean = false
}
