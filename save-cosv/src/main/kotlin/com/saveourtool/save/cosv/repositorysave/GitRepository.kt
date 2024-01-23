package com.saveourtool.save.cosv.repositorysave

import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.spring.repository.BaseEntityRepository
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
}
