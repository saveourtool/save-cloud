@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.save.backend.service

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.vulnerabilities.Vulnerability
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.info.UserPermissions
import org.springframework.security.core.Authentication

/**
 * Interface for service to get required info for COSV from backend
 */
@Suppress("CLASS_NAME_INCORRECT")
interface IBackendService {
    /**
     * @param name name of vulnerability
     * @return vulnerability by name
     */
    fun findVulnerabilityByName(name: String): Vulnerability?

    /**
     * @param vulnerabilityDto dto of new vulnerability
     * @param user who saves [Vulnerability]
     * @return saved [Vulnerability]
     */
    fun saveVulnerability(
        vulnerabilityDto: VulnerabilityDto,
        user: User,
    ): Vulnerability

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
}
