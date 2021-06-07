package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.entities.Project
import org.springframework.data.domain.Example
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
    fun saveProject(project: Project): Long {
        var projectId: Long? = null
        projectRepository.findOne(Example.of(project)).ifPresentOrElse({
            projectId = it.id
        }, {
            val savedProject = projectRepository.save(project)
            projectId = savedProject.id
        })
        return requireNotNull(projectId) { "Should have gotten an ID for project from the database" }
    }

    /**
     * @return list of all projects
     */
    fun getProjects(): List<Project> = projectRepository.findAll()

    fun getProjectByNameAndOwner(name: String, owner: String) = projectRepository.findByNameAndOwner(name, owner)
}
