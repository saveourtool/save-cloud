package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.GitRepo
import com.saveourtool.save.demo.repository.GitRepoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GitRepoService(
    private val gitRepoRepository: GitRepoRepository,
) {
    /**
     * @return
     */
    fun getRepos(): List<GitRepo> = gitRepoRepository.findAll()

    /**
     * @param gitRepo
     */
    fun save(gitRepo: GitRepo): GitRepo = gitRepoRepository.save(gitRepo)

    /**
     * @param gitRepo
     * @return
     */
    @Transactional
    fun saveIfNotPresent(gitRepo: GitRepo): GitRepo = gitRepoRepository.findByToolNameAndOrganizationName(
        gitRepo.toolName,
        gitRepo.organizationName,
    ) ?: save(gitRepo)
}
