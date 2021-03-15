package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Service

@Service
class ProjectService (private val projectRepository: ProjectRepository) {

    fun addResults(project: Project) {
        projectRepository.save(project)
    }
}