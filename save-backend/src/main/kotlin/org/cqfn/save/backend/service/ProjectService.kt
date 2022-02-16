package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.security.Permission
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.domain.ProjectSaveStatus
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectStatus
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
) {
    /**
     * Store [project] in the database
     *
     * @param project a [Project] to store
     * @return project's id, should never return null
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveProject(project: Project): Pair<Long, ProjectSaveStatus> {
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
     * @return project's without status
     */
    fun getNotDeletedProjects(): List<Project> {
        val projects = projectRepository.findAll { root, _, cb ->
            cb.notEqual(root.get<String>("status"), ProjectStatus.DELETED)
        }
        return projects
    }

    /**
     * @return `Mono` with project; `Mono.error` if project cannot be accessed by the current user.
     */
    @Transactional(readOnly = true)
    fun findWithPermissionByNameAndOrganization(
        authentication: Authentication,
        name: String,
        organization: Organization,
        permission: Permission,
        messageIfNotFound: String? = null,
        statusIfForbidden: HttpStatus = HttpStatus.FORBIDDEN,
    ): Mono<Project> = with(projectPermissionEvaluator) {
        Mono.fromCallable { findByNameAndOrganization(name, organization) }
            .switchIfEmpty {
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, messageIfNotFound))
            }
            .filterByPermission(authentication, permission, statusIfForbidden)
    }
}
