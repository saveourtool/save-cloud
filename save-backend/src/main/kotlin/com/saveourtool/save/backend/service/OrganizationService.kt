package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.OrganizationRepository
import com.saveourtool.save.domain.OrganizationSaveStatus
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for organization
 *
 * @property organizationRepository
 */
@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
) {
    /**
     * Store [organization] in the database
     *
     * @param organization an [Organization] to store
     * @return organization's id, should never return null
     */
    @Suppress("UnsafeCallOnNullableType", "TooGenericExceptionCaught")
    @Transactional
    fun saveOrganization(organization: Organization): Pair<Long, OrganizationSaveStatus> {
        val (organizationId, organizationSaveStatus) = if (organizationRepository.validateName(organization.name) != 0L) {
            organizationRepository.saveHighLevelName(organization.name)
            Pair(organizationRepository.save(organization).id, OrganizationSaveStatus.NEW)
        } else {
            Pair(0L, OrganizationSaveStatus.CONFLICT)
        }
        requireNotNull(organizationId) { "Should have gotten an ID for organization from the database" }
        return Pair(organizationId, organizationSaveStatus)
    }

    /**
     * Mark organization with [organizationName] as deleted
     *
     * @param organizationName an [Organization]'s name to delete
     * @return deleted organization
     */
    @Suppress("UnsafeCallOnNullableType")
    fun deleteOrganization(organizationName: String): Organization = getByName(organizationName)
        .apply {
            status = OrganizationStatus.DELETED
        }
        .let {
            organizationRepository.save(it)
        }

    /**
     * @param name
     * @return not deleted Organizations
     */
    fun getNotDeletedOrganizations(name: String?): List<Organization> {
        val organizations = organizationRepository.findAll { root, _, cb ->
            val namePredicate = name?.let { cb.equal(root.get<String>("name"), name) } ?: cb.and()
            cb.and(
                namePredicate,
                cb.notEqual(root.get<String>("status"), OrganizationStatus.DELETED)
            )
        }
        return organizations
    }

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

    /**
     * @param name
     * @return organization by name
     * @throws NoSuchElementException
     */
    fun getByName(name: String) = findByName(name)
        ?: throw NoSuchElementException("There is no organization with name $name.")

    /**
     * @param organization
     * @return organization
     */
    fun updateOrganization(organization: Organization): Organization = organizationRepository.save(organization)

    /**
     * @param name
     * @param relativePath
     * @throws NoSuchElementException
     */
    fun saveAvatar(name: String, relativePath: String) {
        val organization = organizationRepository.findByName(name)?.apply {
            avatar = relativePath
        } ?: throw NoSuchElementException("Organization with name [$name] was not found.")
        organization.let { organizationRepository.save(it) }
    }

    /**
     * @param ownerId
     * @return list of organization by owner id
     */
    fun findByOwnerId(ownerId: Long) = organizationRepository.findByOwnerId(ownerId)

    /**
     * @return all organizations that were registered in SAVE
     */
    fun findAll(): List<Organization> = organizationRepository.findAll()
}
