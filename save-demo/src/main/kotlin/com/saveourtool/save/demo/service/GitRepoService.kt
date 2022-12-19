package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.GitRepo
import com.saveourtool.save.demo.repository.GitRepoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [Service] for [GitRepo] entity
 */
@Service
class GitRepoService(
    private val gitRepoRepository: GitRepoRepository,
) {
    /**
     * @return list of all [GitRepo]s present in database
     */
    fun getRepos(): List<GitRepo> = gitRepoRepository.findAll()

    private fun save(gitRepo: GitRepo): GitRepo = gitRepoRepository.save(gitRepo)

    /**
     * @param gitRepo entity to be saved
     * @return [GitRepo] entity saved to database
     */
    @Transactional
    fun saveIfNotPresent(gitRepo: GitRepo): GitRepo = gitRepoRepository.findByToolNameAndOrganizationName(
        gitRepo.toolName,
        gitRepo.organizationName,
    ) ?: save(gitRepo)
}
