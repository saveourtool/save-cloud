package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Service

/**
 * Service of git
 */
@Service
class GitService(private val gitRepository: GitRepository) {
    /**
     * @param project
     * @return git dto by project if exists
     */
    fun getRepositoryByProject(project: Project) = gitRepository.findByProject(project)?.toDto()
}
