package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.ProjectSaveStatus
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectDto
import org.cqfn.save.entities.ProjectStatus
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException

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
     * @return project's id, should never return null
     */
    fun saveProject(projectDto: ProjectDto): Pair<Long, ProjectSaveStatus> {
        val project = Project.fromDto(projectDto).apply {
            user = userRepository.findByName(projectDto.username).orElseThrow {
                IllegalArgumentException("Attempt to create a project for a non existing user ${projectDto.username}")
            }
        }
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
    fun getProjects(): List<Project> = projectRepository.findAll()

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
}
