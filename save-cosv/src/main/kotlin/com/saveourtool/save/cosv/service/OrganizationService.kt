package com.saveourtool.save.cosv.service

import com.saveourtool.save.cosv.repository.OrganizationRepository
import com.saveourtool.save.entities.Organization

/**
 * Service for organization
 */
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
) {
    /**
     * @param organization organization for update
     * @return updated organization
     */
    fun saveUser(organization: Organization) = organizationRepository.updateOrganization(organization.name, organization.rating)

    /**
     * @param name
     * @return organization with [name]
     */
    fun getOrganizationByName(name: String): Organization = organizationRepository.getOrganizationByName(name)
}
