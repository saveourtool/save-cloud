package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TagRepository
import com.saveourtool.save.cosv.repository.LnkVulnerabilityMetadataTagRepository
import com.saveourtool.save.cosv.repository.VulnerabilityMetadataRepository
import com.saveourtool.save.entities.Tag
import com.saveourtool.save.entities.cosv.LnkVulnerabilityMetadataTag
import com.saveourtool.save.utils.error
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.orNotFound
import com.saveourtool.save.validation.isValidTag

import org.slf4j.Logger
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [Service] for [Tag] entity
 *
 * @property tagRepository
 */
@Service
class TagService(
    private val tagRepository: TagRepository,
    private val vulnerabilityMetadataRepository: VulnerabilityMetadataRepository,
    private val lnkVulnerabilityMetadataTagRepository: LnkVulnerabilityMetadataTagRepository,
) {
    /**
     * @param identifier [com.saveourtool.save.entities.cosv.VulnerabilityMetadata.identifier]
     * @param tagName tag to add
     * @return new [LnkVulnerabilityMetadataTag]
     */
    @Transactional
    fun addVulnerabilityTag(identifier: String, tagName: String): LnkVulnerabilityMetadataTag? {
        if (!tagName.isValidTag()) {
            log.error { "Tag $tagName length should be in [2, 15] range, no commas are allowed." }
            return null
        }
        val metadata = vulnerabilityMetadataRepository.findByIdentifier(identifier).orNotFound {
            "Could not find metadata for vulnerability $identifier"
        }
        val tag = tagRepository.findByName(tagName) ?: tagRepository.save(Tag(tagName))

        return lnkVulnerabilityMetadataTagRepository.save(
            LnkVulnerabilityMetadataTag(metadata, tag)
        )
    }

    /**
     * @param identifier [com.saveourtool.save.entities.cosv.VulnerabilityMetadata.identifier]
     * @param tagName tag to delete
     */
    @Transactional
    fun deleteVulnerabilityTag(identifier: String, tagName: String) {
        val metadata = vulnerabilityMetadataRepository.findByIdentifier(identifier).orNotFound {
            "Could not find metadata for vulnerability $identifier"
        }

        val link = lnkVulnerabilityMetadataTagRepository.findByVulnerabilityMetadataIdAndTagName(
            metadata.requiredId(),
            tagName
        ).orNotFound { "Tag '$tagName' is not linked with vulnerability $identifier." }

        lnkVulnerabilityMetadataTagRepository.delete(link)
    }

    /**
     * @param prefix [String] that should be matched with tag prefix
     * @param page [Pageable]
     * @return [List] of [Tag]s with [Tag.name] that starts with [prefix]
     */
    fun getVulnerabilityTagsByPrefix(
        prefix: String,
        page: Pageable,
    ) = lnkVulnerabilityMetadataTagRepository.findAllByTagNameStartingWith(prefix, page).map { it.tag }

    companion object {
        private val log: Logger = getLogger<TagService>()
    }
}
