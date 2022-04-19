package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.LnkUserProjectRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.domain.ProjectSaveStatus
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectStatus
import org.cqfn.save.permission.Permission

import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Service for project
 *
 * @property projectRepository
 */
@Service
class ProjectService(private val projectRepository: ProjectRepository,
                     private val projectPermissionEvaluator: ProjectPermissionEvaluator,
                     private val lnkUserProjectRepository: LnkUserProjectRepository,
                     private val userRepository: UserRepository
) {
    /**
     * Store [project] in the database
     *
     * @param project a [Project] to store
     * @return project's id, should never return null
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getOrSaveProject(project: Project): Pair<Long, ProjectSaveStatus> {
        val exampleMatcher = ExampleMatcher.matchingAll()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.exact())
            .withMatcher("owner", ExampleMatcher.GenericPropertyMatchers.exact())
        val (projectId, projectSaveStatus) = projectRepository.findOne(Example.of(project, exampleMatcher)).map {
            Pair(it.id, ProjectSaveStatus.EXIST)
        }.orElseGet {
            val savedProject = projectRepository.save(project)
            Pair(savedProject.id, ProjectSaveStatus.NEW)
        }
        requireNotNull(projectId) { "Should have gotten an ID for project from the database" }
        return Pair(projectId, projectSaveStatus)
    }

    /**
     * @return list of all projects
     */
    fun getProjects(): Flux<Project> = projectRepository.findAll().let { Flux.fromIterable(it) }

    /**
     * @param name
     * @param organization
     */
    @Suppress("KDOC_WITHOUT_RETURN_TAG")  // remove after new release of diktat
    fun findByNameAndOrganization(name: String, organization: Organization) = projectRepository.findByNameAndOrganization(name, organization)

    /**
     * @param name
     * @param organizationName
     */
    fun findByNameAndOrganizationName(name: String, organizationName: String) = projectRepository.findByNameAndOrganizationName(name, organizationName)

    /**
     * @param organizationName
     */
    fun findByOrganizationName(organizationName: String) = projectRepository.findByOrganizationName(organizationName).let { Flux.fromIterable(it) }

    /**
     * @return project's without status
     */
    fun getNotDeletedProjects(): List<Project> {
        val projects = projectRepository.findAll { root, _, cb ->
            cb.notEqual(root.get<String>("status"), ProjectStatus.DELETED)
        }
        return projects
    }

    /**
     * @param authentication [Authentication] of the user who wants to access the project
     * @param name name of the project
     * @param organization organization that owns the project
     * @param permission requested [Permission]
     * @param messageIfNotFound if project is not found, include this into 404 response body
     * @param statusIfForbidden return this status if permission is not allowed fot the current user
     * @return `Mono` with project; `Mono.error` if project cannot be accessed by the current user.
     */
    @Transactional(readOnly = true)
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
    fun findWithPermissionByNameAndOrganization(
        authentication: Authentication,
        name: String,
        organization: Organization,
        permission: Permission,
        messageIfNotFound: String? = null,
        statusIfForbidden: HttpStatus = HttpStatus.FORBIDDEN,
    ): Mono<Project> = with(projectPermissionEvaluator) {
        Mono.fromCallable { findByNameAndOrganizationName(name, organization.name) }
            .switchIfEmpty {
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, messageIfNotFound))
            }
            .filterByPermission(authentication, permission, statusIfForbidden)
    }

    /**
     * @param project
     * @param userId
     * @param otherUserName
     * @param requestedRole
     * @return true if user can change roles in project and false otherwise
     */
    @Suppress("UnsafeCallOnNullableType")
    fun canChangeRoles(
        project: Project,
        userId: Long,
        otherUserName: String,
        requestedRole: Role = Role.NONE
    ): Boolean {
        val selfRole = lnkUserProjectRepository.findByUserIdAndProject(userId, project)?.role ?: Role.NONE
        val otherUserId = userRepository.findByName(otherUserName).get().id!!
        val otherRole = lnkUserProjectRepository.findByUserIdAndProject(otherUserId, project)?.role ?: Role.NONE
        return isProjectAdminOrHigher(selfRole) && hasAnotherUserLessPermissions(selfRole, otherRole) &&
                isRequestedPermissionsCanBeSetByUser(selfRole, requestedRole)
    }

    private fun hasAnotherUserLessPermissions(selfRole: Role, otherRole: Role): Boolean = selfRole.priority > otherRole.priority

    private fun isRequestedPermissionsCanBeSetByUser(selfRole: Role, requestedRole: Role): Boolean = selfRole.priority > requestedRole.priority

    private fun isProjectAdminOrHigher(userRole: Role): Boolean = userRole.priority >= Role.ADMIN.priority
}
