package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TagRepository
import com.saveourtool.save.backend.repository.vulnerability.LnkVulnerabilityTagRepository
import com.saveourtool.save.backend.repository.vulnerability.VulnerabilityRepository
import com.saveourtool.save.entities.Tag
import com.saveourtool.save.entities.vulnerabilities.LnkVulnerabilityTag
import com.saveourtool.save.entities.vulnerabilities.Vulnerability
import com.saveourtool.save.utils.orNotFound
import com.saveourtool.save.validation.TAG_ERROR_MESSAGE
import com.saveourtool.save.validation.isValidTag
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * [Service] for [Tag] entity
 *
 * @property tagRepository
 */
@Service
class TagService(
    private val tagRepository: TagRepository,
    private val vulnerabilityRepository: VulnerabilityRepository,
    private val lnkVulnerabilityTagRepository: LnkVulnerabilityTagRepository,
) {
    /**
     * @param identifier [Vulnerability.identifier]
     * @param tagName tag to add
     * @return new [LnkVulnerabilityTag]
     * @throws ResponseStatusException on invalid [tagName] (with [HttpStatus.CONFLICT])
     */
    @Transactional
    fun addVulnerabilityTag(identifier: String, tagName: String): LnkVulnerabilityTag {
        if (!tagName.isValidTag()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, TAG_ERROR_MESSAGE)
        }
        val vulnerability = vulnerabilityRepository.findByIdentifier(identifier).orNotFound {
            "Could not find vulnerability $identifier"
        }
        val tag = tagRepository.findByName(tagName) ?: tagRepository.save(Tag(tagName))

        return lnkVulnerabilityTagRepository.save(
            LnkVulnerabilityTag(vulnerability, tag)
        )
    }

    /**
     * @param identifier [Vulnerability.identifier]
     * @param tagName tag to delete
     * @return updated [Vulnerability]
     */
    @Transactional
    fun deleteVulnerabilityTag(identifier: String, tagName: String) {
        val vulnerability = vulnerabilityRepository.findByIdentifier(identifier).orNotFound {
            "Could not find vulnerability $identifier"
        }

        val link = lnkVulnerabilityTagRepository.findByVulnerabilityIdAndTagName(
            vulnerability.requiredId(),
            tagName
        ).orNotFound { "Tag '$tagName' is not linked with vulnerability $identifier." }

        lnkVulnerabilityTagRepository.delete(link)
    }

    /**
     * @param prefix [String] that should be matched with tag prefix
     * @param page [Pageable]
     * @return [List] of [Tag]s with [Tag.name] that starts with [prefix]
     */
    fun getVulnerabilityTagsByPrefix(
        prefix: String,
        page: Pageable,
    ) = lnkVulnerabilityTagRepository.findAllByTagNameStartingWith(prefix, page).map { it.tag }
}
