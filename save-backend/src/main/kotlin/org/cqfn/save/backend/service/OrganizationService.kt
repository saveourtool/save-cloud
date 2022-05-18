package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.OrganizationRepository
import org.cqfn.save.domain.OrganizationSaveStatus
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.OrganizationStatus
import org.springframework.stereotype.Service

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
    @Suppress("UnsafeCallOnNullableType")
    fun getOrSaveOrganization(organization: Organization): Pair<Long, OrganizationSaveStatus> {
        val (organizationId, organizationSaveStatus) = organizationRepository.findByName(organization.name)?.let {
            Pair(it.id, OrganizationSaveStatus.EXIST)
        } ?: Pair(organizationRepository.save(organization).id, OrganizationSaveStatus.NEW)
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
    fun deleteOrganization(organizationName: String) =
            organizationRepository.findByName(organizationName)
                ?.apply {
                    status = OrganizationStatus.DELETED
                }
                ?.let {
                    organizationRepository.save(it)
                } ?: throw NoSuchElementException("There is no organization with name $organizationName.")

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
}
