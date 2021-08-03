package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.entities.Git
import org.cqfn.save.entities.GitDto
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
    fun getRepositoryDtoByProject(project: Project) = gitRepository.findByProject(project)?.toDto()

    /**
     * @param gitDto
     * @param project
     */
    @Suppress("KDOC_WITHOUT_RETURN_TAG")  // https://github.com/cqfn/diKTat/issues/965
    fun saveGit(gitDto: GitDto, project: Project) =
            gitRepository.save(Git(gitDto.url, gitDto.username, gitDto.password, gitDto.branch, project))
}
