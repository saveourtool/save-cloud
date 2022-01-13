package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.ProjectSaveStatus
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectStatus
import org.cqfn.save.entities.User
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Service for project
 *
 * @property projectRepository
 */
@Service
class ProjectService(private val projectRepository: ProjectRepository,
                     private val userRepository: UserRepository,
) {
    /**
     * Store [project] in the database
     *
     * @param project a [Project] to store
     * @param username name of the user that should be associated as a creator of this project. If null, [project] should contain valid user id.
     * @return project's id, should never return null
     * @throws ResponseStatusException if project doesn't exist and no username has been provided
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveProject(project: Project, username: String?): Pair<Long, ProjectSaveStatus> {
        val exampleMatcher = ExampleMatcher.matchingAll()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.exact())
            .withMatcher("owner", ExampleMatcher.GenericPropertyMatchers.exact())
        val (projectId, projectSaveStatus) = projectRepository.findOne(Example.of(project, exampleMatcher)).map {
            Pair(it.id, ProjectSaveStatus.EXIST)
        }.orElseGet {
            // if project is not found, add mapping to a user and save it
            username?.let {
                userRepository.findByName(username)
                    .map {
                        project.userId = it.id!!
                        it
                    }
                    .orElseThrow {
                        ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt to create project for a non-existent user $username")
                    }
            }
            // if no username is provided, then we are trying to update the project. Then, if the project is not found,
            // this is an error. Todo: make this logic more obvious
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt to update non-existent project")
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
     * @param owner
     */
    @Suppress("KDOC_WITHOUT_RETURN_TAG")  // remove after new release of diktat
    fun findByNameAndOwner(name: String, owner: String) = projectRepository.findByNameAndOwner(name, owner)

    /**
     * @return project's without status
     */
    fun getNotDeletedProjects(): List<Project> {
        val projects = projectRepository.findAll { root, _, cb ->
            cb.notEqual(root.get<String>("status"), ProjectStatus.DELETED)
        }
        return projects
    }

    fun hasWriteAccess(username: String, project: Project): Boolean {
        return userRepository.findByName(username).map { user ->
            project.userId == user.id!! ||
                    user.id!! in project.adminIdList()
        }.orElse(false)
    }
}
