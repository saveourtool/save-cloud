package com.saveourtool.save.backend.security

import com.saveourtool.save.authservice.utils.username
import com.saveourtool.save.backend.service.LnkUserOrganizationService
import com.saveourtool.save.info.UserPermissions
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing user's permissions regarding.
 */
@Component
class UserPermissionEvaluator(
    private var lnkUserOrganizationService: LnkUserOrganizationService,
) {
    /**
     * @param authentication
     * @return list of UserPermissions in organizations
     */
    fun getUserPermissions(
        authentication: Authentication,
    ): List<UserPermissions> {
        val lnkOrganizations = lnkUserOrganizationService.getOrganizationsByUserNameAndCreatedStatus(authentication.username())

        return lnkOrganizations.map {
            UserPermissions(
                it.organization.canCreateContests,
                it.organization.canBulkUpload,
                it.organization.name,
            )
        }
    }

    /**
     * @param authentication
     * @param organizationName
     * @return UserPermissions
     */
    fun getUserPermissions(
        authentication: Authentication,
        organizationName: String,
    ): UserPermissions {
        val lnkOrganization = lnkUserOrganizationService.getOrganizationsByUserNameAndCreatedStatusAndOrganizationName(authentication.username(), organizationName)

        val isPermittedCreateContest = lnkOrganization?.organization?.canCreateContests ?: false
        val isPermittedToBulkUpload = lnkOrganization?.organization?.canBulkUpload ?: false

        return UserPermissions(
            isPermittedCreateContest,
            isPermittedToBulkUpload,
            organizationName,
        )
    }
}
