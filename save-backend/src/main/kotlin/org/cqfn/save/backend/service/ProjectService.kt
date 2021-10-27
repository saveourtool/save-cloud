package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.domain.ProjectSaveStatus
import org.cqfn.save.entities.Project
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.stereotype.Service

/**
 * Service for project
 *
 * @property projectRepository
 */
@Service
class ProjectService(private val projectRepository: ProjectRepository) {
    /**
     * Store [project] in the database
     *
     * @param project a [Project] to store
     * @return project's id, should never return null
     */
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
    fun getProjects(): List<Project> = projectRepository.findAll()

    /**
     * @param name
     * @param owner
     */
    @Suppress("KDOC_WITHOUT_RETURN_TAG")  // https://github.com/cqfn/diKTat/issues/965
    fun getProjectByNameAndOwner(name: String, owner: String) = projectRepository.findByNameAndOwner(name, owner)

    /**
     * @param status status project
     * @return project's without status
     */
    fun getNotDeletedProjects(status: String): List<Project> {
        val projects = projectRepository.findAll { root, _, cb ->
            cb.notEqual(root.get<String>("status"), status)
        }
        return projects
    }
}
