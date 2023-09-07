@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.save.backend.service

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserPermissions
import org.springframework.security.core.Authentication

/**
 * Interface for service to get required info for COSV from backend
 */
@Suppress("CLASS_NAME_INCORRECT")
interface IBackendService {
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
