package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.security.OrganizationPermissionEvaluator
import com.saveourtool.save.backend.security.UserPermissionEvaluator
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.LnkVulnerabilityMetadataTag
import com.saveourtool.save.permission.Permission
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.nio.file.Path

/**
 * Service for [IBackendService] to get required info for COSV from backend
 */
@Service
class BackendForCosvService(
    private val organizationService: OrganizationService,
    private val userDetailsService: UserDetailsService,
    private val userPermissionEvaluator: UserPermissionEvaluator,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
    private val tagService: TagService,
    private val lnkUserOrganizationService: LnkUserOrganizationService,
    configProperties: ConfigProperties,
) : IBackendService {
    override val workingDir: Path = configProperties.workingDir

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

    override fun addVulnerabilityTags(
        identifier: String,
        tagName: Set<String>
    ): List<LnkVulnerabilityMetadataTag>? = tagService.addVulnerabilityTags(identifier, tagName)

    override fun addVulnerabilityTag(
        identifier: String,
        tagName: String
    ): LnkVulnerabilityMetadataTag = tagService.addVulnerabilityTag(identifier, tagName)

    override fun deleteVulnerabilityTag(
        identifier: String,
        tagName: String
    ) = tagService.deleteVulnerabilityTag(identifier, tagName)

    override fun getGlobalRoleOrOrganizationRole(
        authentication: Authentication,
        organizationName: String,
    ): Role = lnkUserOrganizationService.getGlobalRoleOrOrganizationRole(authentication, organizationName)
}
