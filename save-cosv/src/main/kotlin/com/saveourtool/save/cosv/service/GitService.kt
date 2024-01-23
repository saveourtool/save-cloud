package com.saveourtool.save.cosv.service

import com.saveourtool.save.cosv.repositorysave.GitRepository
import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Organization
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/**
 * Service of git
 */
@Service
class GitService(private val gitRepository: GitRepository) {
    /**
     * @param organization
     * @return list of gits by organization if exists
     */
    fun getAllByOrganization(organization: Organization): List<Git> = gitRepository.findAllByOrganizationId(organization.requiredId())

    /**
     * @param organization
     * @param url
     * @return list of gits by organization if exists or null
     */
    fun findByOrganizationAndUrl(organization: Organization, url: String): Git? = gitRepository.findByOrganizationAndUrl(organization, url)

    /**
     * @param organization
     * @param url
     * @return list of gits by organization if exists
     * @throws NoSuchElementException
     */
    fun getByOrganizationAndUrl(organization: Organization, url: String): Git = findByOrganizationAndUrl(organization, url)
        ?: throw NoSuchElementException("There is no git credential with url $url in ${organization.name}")

    /**
     * @param git
     * @return saved or updated git
     */
    fun save(git: Git): Git = gitRepository.save(git)

    /**
     * @param organization
     * @param url git url
     * @throws NoSuchElementException
     */
    fun delete(organization: Organization, url: String) {
        gitRepository.delete(getByOrganizationAndUrl(organization, url))
    }

    /**
     * @param id
     * @return [GitDto] found by provided values or null
     */
    fun findById(id: Long): GitDto? = gitRepository.findByIdOrNull(id)?.toDto()
}
