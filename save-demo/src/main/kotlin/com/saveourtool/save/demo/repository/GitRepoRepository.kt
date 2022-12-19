package com.saveourtool.save.demo.repository

import com.saveourtool.save.demo.entity.GitRepo
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [GitRepo] entity.
 */
@Repository
interface GitRepoRepository : BaseEntityRepository<GitRepo> {
    /**
     * @param organizationName
     * @return list of [GitRepo] by [organizationName]
     */
    fun findByOrganizationName(organizationName: String): List<GitRepo>

    /**
     * @param toolName
     * @param organizationName
     * @return [GitRepo] if an entity with such [toolName] and [organizationName] was found in database, null otherwise
     */
    fun findByToolNameAndOrganizationName(toolName: String, organizationName: String): GitRepo?
}
