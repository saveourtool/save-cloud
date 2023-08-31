@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.save.backend.service

import com.saveourtool.save.entities.Organization

/**
 * Interface for OrganizationService
 */
@Suppress("CLASS_NAME_INCORRECT")
interface IOrganizationService {
    /**
     * @param name name of organization
     * @return found [Organization] by name
     */
    fun getByName(name: String): Organization
}
