package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.Organization
import org.springframework.stereotype.Repository

/**
 * Repository of git
 */
@Repository
interface GitRepository : BaseEntityRepository<Git> {
    /**
     * @param organizationId
     * @return list of gits by [organizationId]
     */
    fun findAllByOrganizationId(organizationId: Long): List<Git>

    /**
     * @param organization
     * @param url
     * @return git by [organizationId] and [url]
     */
    fun findByOrganizationAndUrl(organization: Organization, url: String): Git?

    /**
     * @param url
     * @return all [Git] entities by [Git.url]
     */
    fun findAllByUrl(url: String): List<Git>
}
