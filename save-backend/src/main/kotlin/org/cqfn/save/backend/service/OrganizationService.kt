package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.OrganizationRepository
import org.springframework.stereotype.Service

/**
 * Service for organization
 *
 * @property organizationRepository
 */
@Service
class OrganizationService(private val organizationRepository: OrganizationRepository) {
    /**
     * @param organizationId
     * @return organization by id
     */
    fun getOrganizationById(organizationId: Long) = organizationRepository.getOrganizationById(organizationId)

    /**
     * @param name
     * @return organization by name
     */
    fun findByName(name: String) = organizationRepository.findByName(name)
}
