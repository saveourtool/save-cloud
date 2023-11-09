@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.save.backend.service

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.LnkVulnerabilityMetadataTag
import com.saveourtool.save.info.UserPermissions
import com.saveourtool.save.permission.Permission
import org.jetbrains.annotations.Blocking
import org.springframework.security.core.Authentication
import java.nio.file.Path

/**
 * Interface for service to get required info for COSV from backend
 */
@Suppress("CLASS_NAME_INCORRECT")
interface IBackendService {
    /**
     * Working directory for backend
     */
    val workingDir: Path

    /**
     * @param user user for update
     * @return updated user
     */
    fun saveUser(user: User): User

    /**
     * @param organization organization for update
     * @return updated organization
     */
    fun saveOrganization(organization: Organization): Organization

    /**
     * @param name name of organization
     * @return found [Organization] by name
     */
    fun getOrganizationByName(name: String): Organization

    /**
     * @param name name of organization
     * @return found [User] by name
     */
    fun getUserByName(name: String): User

    /**
     * @param authentication
     * @param organizationName name of organization
     * @return found [UserPermissions] by organizationName
     */
    fun getUserPermissionsByOrganizationName(authentication: Authentication, organizationName: String): UserPermissions

    /**
     * @param authentication
     * @param organizationName name of organization
     * @param permission
     * @return true if [authentication] has [permission] in [organizationName], otherwise -- false
     */
    @Blocking
    fun hasPermissionInOrganization(
        authentication: Authentication,
        organizationName: String,
        permission: Permission,
    ): Boolean

    /**
     * @param identifier [com.saveourtool.save.entities.cosv.VulnerabilityMetadata.identifier]
     * @param tagName tag to add
     * @return new [LnkVulnerabilityMetadataTag]
     */
    fun addVulnerabilityTags(
        identifier: String,
        tagName: Set<String>
    ): List<LnkVulnerabilityMetadataTag>?
}
