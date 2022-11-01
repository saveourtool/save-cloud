package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.OrganizationRepository
import com.saveourtool.save.domain.OrganizationSaveStatus
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.ProjectStatus
import com.saveourtool.save.filters.OrganizationFilters
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for organization
 *
 * @property organizationRepository
 */
@Service
class OrganizationService(
    private val projectService: ProjectService,
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
     * Mark organization with [organizationName] as deleted or banned
     *
     * @param organizationName an [Organization]'s name to delete or banned
     * @param status on [deleted] or [banned]
     * @return deleted organization
     */
    @Suppress("UnsafeCallOnNullableType")
    fun deleteOrganization(organizationName: String, status: OrganizationStatus): Organization {
        if (status == OrganizationStatus.BANNED) {
            val projects = projectService.getAllByOrganizationName(organizationName)
            projects.forEach {
                it.status = ProjectStatus.BANNED
                projectService.updateProject(it)
            }
        }
        return changeOrganizationStatus(organizationName, status)
    }

    /**
     * Mark organization with [organizationName] as recover
     *
     * @param organizationName an [Organization]'s name to recovery
     * @return deleted organization
     */
    fun recoverOrganization(organizationName: String): Organization =
            changeOrganizationStatus(organizationName, OrganizationStatus.CREATED)

    /**
     * Mark organization with [organizationName] as recover
     *
     * @param organizationName an [Organization]'s name to recovery
     * @param changeStatus - the status to be assigned to the [Organization]
     * @return deleted organization
     */
    @Suppress("UnsafeCallOnNullableType")
    fun changeOrganizationStatus(organizationName: String, changeStatus: OrganizationStatus): Organization = getByName(organizationName)
        .apply {
            status = changeStatus
        }
        .let {
            organizationRepository.save(it)
        }

    /**
     * @param organizationFilters
     * @return list of organizations with that match [organizationFilters]
     */
    fun getFiltered(organizationFilters: OrganizationFilters) = if (organizationFilters.prefix.isBlank()) {
        organizationRepository.findByStatus(organizationFilters.status)
    } else {
        organizationRepository.findByNameStartingWithAndStatus(
            organizationFilters.prefix,
            organizationFilters.status,
        )
    }

    /**
     * @param organizationFilters
     * @return not deleted Organizations
     */
    fun getCreatedOrganizations(organizationFilters: OrganizationFilters?): List<Organization> {
        val name = organizationFilters?.prefix?.let { "%$it%" }
        val organizations = organizationRepository.findAll { root, _, cb ->
            val namePredicate = name?.let { cb.like(root.get("name"), it) } ?: cb.and()
            cb.and(
                namePredicate,
                cb.equal(root.get<String>("status"), OrganizationStatus.CREATED)
            )
        }
        return organizations
    }

    /**
     * @param organizationName
     * @return [true] if number of Organization projects is zero, else - [false]
     */
    fun organizationHasNoProjects(organizationName: String) = numberOfProjectInOrganization(organizationName) == 0

    /**
     * @param organizationName
     * @return number of Organization projects
     */
    fun numberOfProjectInOrganization(organizationName: String) = projectService.getAllByOrganizationName(organizationName).count { it.status == ProjectStatus.CREATED }

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

    /**
     * @param organizationName
     * @param authentication
     * @return global rating of organization by name [organizationName] based on ratings of all projects under this organization
     */
    fun getGlobalRating(organizationName: String, authentication: Authentication) =
            projectService.getNotDeletedProjectsByOrganizationName(organizationName, authentication)
                .collectList()
                .map { projectsList ->
                    projectsList.sumOf { it.contestRating }
                }
}
