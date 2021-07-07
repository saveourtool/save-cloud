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
    @Suppress("UnsafeCallOnNullableType")
    fun saveProject(project: Project): Pair<Long, ProjectSaveStatus> {
        val exampleMatcher = ExampleMatcher.matchingAll()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.exact())
            .withMatcher("owner", ExampleMatcher.GenericPropertyMatchers.exact())
        var projectId: Long? = null
        var projectSaveStatus: ProjectSaveStatus? = null
        projectRepository.findOne(Example.of(project, exampleMatcher)).ifPresentOrElse({
            projectId = it.id
            projectSaveStatus = ProjectSaveStatus.EXIST
        }, {
            val savedProject = projectRepository.save(project)
            projectId = savedProject.id
            projectSaveStatus = ProjectSaveStatus.NEW
        })
        requireNotNull(projectId) { "Should have gotten an ID for project from the database" }
        return Pair(projectId!!, projectSaveStatus!!)
    }

    /**
     * @return list of all projects
     */
    fun getProjects(): List<Project> = projectRepository.findAll()

    /**
     * @param name
     * @param owner
     */
    fun getProjectByNameAndOwner(name: String, owner: String) = projectRepository.findByNameAndOwner(name, owner)
}
