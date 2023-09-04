@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.save.backend.service

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User

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
}
