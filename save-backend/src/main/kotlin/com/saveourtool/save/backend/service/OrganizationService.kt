package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.OrganizationRepository
import com.saveourtool.save.domain.OrganizationSaveStatus
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.ProjectStatus.*
import com.saveourtool.save.filters.OrganizationFilter
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.orNotFound
import com.saveourtool.save.validation.isValidLengthName
import org.springframework.data.domain.Pageable
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.NoSuchElementException

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
        val (organizationId, organizationSaveStatus) = if (!organization.name.isValidLengthName()) {
            Pair(0L, OrganizationSaveStatus.INVALID_NAME)
        } else if (organizationRepository.validateName(organization.name) != 0L) {
            organizationRepository.saveHighLevelName(organization.name)
            Pair(organizationRepository.save(organization).id, OrganizationSaveStatus.NEW)
        } else {
            Pair(0L, OrganizationSaveStatus.CONFLICT)
        }
        requireNotNull(organizationId) { "Should have gotten an ID for organization from the database" }
        return Pair(organizationId, organizationSaveStatus)
    }

    /**
     * Mark organization with [organization] as [newProjectStatus].
     * Before performing the function, check for user permissions by the [organization].
     *
     * @param newProjectStatus is new status for [organization]
     * @param organization is organization in which the status will be changed
     * @return organization
     */
    @Suppress("UnsafeCallOnNullableType")
    private fun changeOrganizationStatus(organization: Organization, newProjectStatus: OrganizationStatus): Organization = organization
        .apply {
            status = newProjectStatus
        }
        .let {
            organizationRepository.save(it)
        }

    /**
     * Mark organization [organization] as deleted
     *
     * @param organization an [Organization] to delete
     * @return deleted organization
     */
    fun deleteOrganization(organization: Organization): Organization = changeOrganizationStatus(organization, OrganizationStatus.DELETED)

    /**
     * Mark organization with [organization] as created.
     * If an organization was previously banned, then all its projects become deleted.
     *
     * @param organization an [Organization] to create
     * @return recovered organization
     */
    @Transactional
    fun recoverOrganization(organization: Organization): Organization {
        if (organization.status == OrganizationStatus.BANNED) {
            projectService.getAllByOrganizationName(organization.name).forEach {
                it.status = DELETED
                projectService.updateProject(it)
            }
        }
        return changeOrganizationStatus(organization, OrganizationStatus.CREATED)
    }

    /**
     * Mark organization with [organization] and all its projects as banned.
     *
     * @param organization an [Organization] to ban
     * @return banned organization
     */
    @Transactional
    fun banOrganization(organization: Organization): Organization {
        projectService.getAllByOrganizationName(organization.name).forEach {
            it.status = BANNED
            projectService.updateProject(it)
        }
        return changeOrganizationStatus(organization, OrganizationStatus.BANNED)
    }

    /**
     * @param organizationName the unique name of the organization.
     * @return `true` if this organization has at least one non-deleted project,
     *   `false` otherwise.
     */
    fun hasProjects(organizationName: String): Boolean =
            projectService.getAllByOrganizationName(organizationName).any { project ->
                project.status == CREATED
            }

    /**
     * @param organizationId
     * @return organization by id
     */
    fun getOrganizationById(organizationId: Long) = organizationRepository.getOrganizationById(organizationId)

    /**
     * @param name
     * @param statuses
     * @return organization by name and statuses
     */
    fun findByNameAndStatuses(name: String, statuses: Set<OrganizationStatus>) =
            organizationRepository.findByNameAndStatusIn(name, statuses)

    /**
     * @param name
     * @return organization by name with [CREATED] status
     */
    fun findByNameAndCreatedStatus(name: String) = findByNameAndStatuses(name, EnumSet.of(OrganizationStatus.CREATED))

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
    fun getByName(name: String) = findByNameAndCreatedStatus(name)
        ?: throw NoSuchElementException("There is no organization with name $name.")

    /**
     * @param organizationFilter
     * @param pageable [Pageable]
     * @return list of organizations with that match [organizationFilter]
     */
    fun getFiltered(organizationFilter: OrganizationFilter, pageable: Pageable): List<Organization> = organizationRepository.findByNameStartingWithAndStatusIn(
        organizationFilter.prefix,
        organizationFilter.statuses,
        pageable,
    )

    /**
     * @param organization
     * @return organization
     */
    fun updateOrganization(organization: Organization): Organization = organizationRepository.save(organization)

    /**
     * We change the version just to work-around the caching on the frontend
     *
     * @param name
     * @return the id (version) of new avatar
     * @throws NoSuchElementException
     */
    fun updateAvatarVersion(name: String): String {
        val organization = organizationRepository.findByName(name).orNotFound()
        var version = organization.avatar?.substringAfterLast("?")?.toInt() ?: 0
        val newAvatar = "${AvatarType.ORGANIZATION.toUrlStr(name)}?${++version}"

        organization.apply {
            avatar = newAvatar
        }.orNotFound { "Organization with name [$name] was not found." }
        organization.let { organizationRepository.save(it) }

        return newAvatar
    }

    /**
     * @return all organizations that were registered in SAVE
     */
    fun findAll(): List<Organization> = organizationRepository.findAll()

    /**
     * @param organizationName
     * @param authentication
     * @return global rating of organization by name [organizationName] based on ratings of all projects under this organization
     */
    fun getGlobalRating(organizationName: String, authentication: Authentication?) =
            projectService.getProjectsByOrganizationNameAndCreatedStatus(organizationName, authentication)
                .collectList()
                .map { projectsList ->
                    projectsList.sumOf { it.contestRating }
                }
}
