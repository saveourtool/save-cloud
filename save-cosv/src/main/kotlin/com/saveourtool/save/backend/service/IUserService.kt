@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.save.backend.service

import com.saveourtool.save.entities.User

/**
 * Interface for OrganizationService
 */
@Suppress("CLASS_NAME_INCORRECT")
interface IUserService {
    /**
     * @param name name of organization
     * @return found [User] by name
     */
    fun getByName(name: String): User
}
