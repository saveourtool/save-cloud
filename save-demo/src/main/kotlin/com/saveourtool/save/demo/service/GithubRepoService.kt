package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.GithubRepo
import com.saveourtool.save.demo.repository.GithubRepoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [Service] for [GithubRepo] entity
 */
@Service
class GithubRepoService(
    private val githubRepoRepository: GithubRepoRepository,
) {
    /**
     * @return list of all [GithubRepo]s present in database
     */
    fun getRepos(): List<GithubRepo> = githubRepoRepository.findAll()

    /**
     * @param ownerName name of GitHub user/organization
     * @param repoName name of GitHub repository
     * @return [GithubRepo] entity
     */
    fun find(ownerName: String, repoName: String): GithubRepo? = githubRepoRepository.findByToolNameAndOrganizationName(repoName, ownerName)

    private fun save(githubRepo: GithubRepo): GithubRepo = githubRepoRepository.save(githubRepo)

    /**
     * @param githubRepo entity to be saved
     * @return [GithubRepo] entity saved to database
     */
    @Transactional
    fun saveIfNotPresent(githubRepo: GithubRepo): GithubRepo = githubRepoRepository.findByToolNameAndOrganizationName(
        githubRepo.toolName,
        githubRepo.organizationName,
    ) ?: save(githubRepo)
}
