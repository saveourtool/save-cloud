package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.security.OrganizationPermissionEvaluator
import com.saveourtool.save.backend.security.UserPermissionEvaluator
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.permission.Permission
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for [IBackendService] to get required info for COSV from backend
 */
@Service
class BackendForCosvService(
    private val organizationService: OrganizationService,
    private val userDetailsService: UserDetailsService,
    private val userPermissionEvaluator: UserPermissionEvaluator,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
) : IBackendService {
    override fun getOrganizationByName(name: String): Organization = organizationService.getByName(name)

    override fun getUserByName(name: String): User = userDetailsService.getByName(name)

    override fun getUserPermissionsByOrganizationName(
        authentication: Authentication,
        organizationName: String,
    ) = userPermissionEvaluator.getUserPermissionsByOrganizationName(authentication, organizationName)

    override fun hasPermissionInOrganization(
        authentication: Authentication,
        organizationName: String,
        permission: Permission
    ): Boolean = organizationPermissionEvaluator.hasPermission(authentication, getOrganizationByName(organizationName), permission)

    override fun saveUser(user: User): User = userDetailsService.saveUser(user)

    override fun saveOrganization(organization: Organization) = organizationService.updateOrganization(organization)

    @Transactional
    override fun addRating(user: User, organization: Organization?, uploadedVulnerabilities: Int) {
        user.apply {
            rating += uploadedVulnerabilities * VULNERABILITY_OWNER_RATING
        }
        userDetailsService.saveUser(user)

        organization?.let {
            organization.apply {
                rating += uploadedVulnerabilities * VULNERABILITY_ORGANIZATION_RATING
            }
            organizationService.updateOrganization(organization)
        }
    }

    companion object {
        private const val VULNERABILITY_ORGANIZATION_RATING = 10
        private const val VULNERABILITY_OWNER_RATING = 10
    }
}
