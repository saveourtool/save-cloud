package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.OrganizationRepository
import org.cqfn.save.domain.OrganizationSaveStatus
import org.cqfn.save.entities.Organization
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.stereotype.Service

/**
 * Service for organization
 *
 * @property organizationRepository
 */
@Service
class OrganizationService(private val organizationRepository: OrganizationRepository) {
    /**
     * Store [organization] in the database
     *
     * @param organization an [Organization] to store
     * @return organization's id, should never return null
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveOrganization(organization: Organization): Pair<Long, OrganizationSaveStatus> {
        val exampleMatcher = ExampleMatcher.matchingAny()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.exact())
        val (organizationId, organizationSaveStatus) = organizationRepository.findOne(Example.of(organization, exampleMatcher)).map {
            Pair(it.id, OrganizationSaveStatus.EXIST)
        }.orElseGet {
            val savedOrganization = organizationRepository.save(organization)
            Pair(savedOrganization.id, OrganizationSaveStatus.NEW)
        }
        requireNotNull(organizationId) { "Should have gotten an ID for organization from the database" }
        return Pair(organizationId, organizationSaveStatus)
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
     * @param relativePath
     */
    fun saveAvatar(name: String, relativePath: String) {
        val organization = organizationRepository.findByName(name).apply {
            avatar = relativePath
        }
        organizationRepository.save(organization)
    }
}
