package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.entities.Project
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitService(private val gitRepository: GitRepository) {

    fun getRepositoryByProject(project : Project) = gitRepository.findByProject(project).toDto()
}
