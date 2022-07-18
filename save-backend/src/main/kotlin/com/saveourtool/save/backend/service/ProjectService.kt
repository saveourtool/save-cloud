package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.ProjectRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.domain.ProjectSaveStatus
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.ProjectStatus
import com.saveourtool.save.entities.User
import com.saveourtool.save.permission.Permission

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
import java.util.Optional

/**
 * Service for project
 *
 * @property projectRepository
 */
@Service
@OptIn(ExperimentalStdlibApi::class)
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
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
     * @param projectName name of the project
     * @param organizationName organization that owns the project
     * @param permission requested [Permission]
     * @param messageIfNotFound if project is not found, include this into 404 response body
     * @param statusIfForbidden return this status if permission is not allowed fot the current user
     * @return `Mono` with project; `Mono.error` if project cannot be accessed by the current user.
     */
    @Transactional(readOnly = true)
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
    fun findWithPermissionByNameAndOrganization(
        authentication: Authentication,
        projectName: String,
        organizationName: String,
        permission: Permission,
        messageIfNotFound: String? = null,
        statusIfForbidden: HttpStatus = HttpStatus.FORBIDDEN,
    ): Mono<Project> = with(projectPermissionEvaluator) {
        Mono.fromCallable { findByNameAndOrganizationName(projectName, organizationName) }
            .switchIfEmpty {
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, messageIfNotFound))
            }
            .filterByPermission(authentication, permission, statusIfForbidden)
    }

    /**
     * @param userName
     * @return optional of user
     */
    fun findUserByName(userName: String): Optional<User> = userRepository.findByName(userName)

    /**
     * @param id
     * @return [Project] with given [id]
     */
    fun findById(id: Long): Optional<Project> = projectRepository.findById(id)
}
