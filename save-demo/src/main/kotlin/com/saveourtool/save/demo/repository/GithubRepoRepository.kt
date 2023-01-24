package com.saveourtool.save.demo.repository

import com.saveourtool.save.demo.entity.GithubRepo
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [GithubRepo] entity.
 */
@Repository
interface GithubRepoRepository : BaseEntityRepository<GithubRepo> {
    /**
     * @param organizationName
     * @return list of [GithubRepo] by [organizationName]
     */
    fun findByOrganizationName(organizationName: String): List<GithubRepo>

    /**
     * @param projectName
     * @param organizationName
     * @return [GithubRepo] if an entity with such [projectName] and [organizationName] was found in database, null otherwise
     */
    fun findByProjectNameAndOrganizationName(projectName: String, organizationName: String): GithubRepo?
}
