package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.GitRepository
import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
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
    fun getRepositoryDtoByProject(project: Project) = gitRepository.findByProject(project)?.toDto()

    /**
     * @param gitDto
     * @param projectId associate Git with this project
     * @return saved git dto
     */
    fun saveGit(gitDto: GitDto, projectId: Long) = gitRepository.save(
        Git(gitDto.url, gitDto.username, gitDto.password, gitDto.branch, Project.stub(projectId))
    )
}
