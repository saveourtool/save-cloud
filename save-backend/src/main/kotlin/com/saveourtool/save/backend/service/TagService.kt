package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TagRepository
import com.saveourtool.save.cosv.repository.CosvMetadataRepository
import com.saveourtool.save.cosv.repository.LnkCosvMetadataTagRepository
import com.saveourtool.save.entities.Tag
import com.saveourtool.save.entities.cosv.LnkCosvMetadataTag
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
    private val cosvMetadataRepository: CosvMetadataRepository,
    private val lnkCosvMetadataTagRepository: LnkCosvMetadataTagRepository,
) {
    /**
     * @param identifier [CosvMetadata.cosvId]
     * @param tagName tag to add
     * @return new [LnkCosvMetadataTag]
     * @throws ResponseStatusException on invalid [tagName] (with [HttpStatus.CONFLICT])
     */
    @Transactional
    fun addVulnerabilityTag(identifier: String, tagName: String): LnkCosvMetadataTag {
        if (!tagName.isValidTag()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, TAG_ERROR_MESSAGE)
        }
        val metadata = cosvMetadataRepository.findByCosvId(identifier).orNotFound {
            "Could not find metadata for vulnerability $identifier"
        }
        val tag = tagRepository.findByName(tagName) ?: tagRepository.save(Tag(tagName))

        return lnkCosvMetadataTagRepository.save(
            LnkCosvMetadataTag(metadata, tag)
        )
    }

    /**
     * @param identifier [CosvMetadata.cosvId]
     * @param tagName tag to delete
     */
    @Transactional
    fun deleteVulnerabilityTag(identifier: String, tagName: String) {
        val metadata = cosvMetadataRepository.findByCosvId(identifier).orNotFound {
            "Could not find metadata for vulnerability $identifier"
        }

        val link = lnkCosvMetadataTagRepository.findByCosvMetadataIdAndTagName(
            metadata.requiredId(),
            tagName
        ).orNotFound { "Tag '$tagName' is not linked with vulnerability $identifier." }

        lnkCosvMetadataTagRepository.delete(link)
    }

    /**
     * @param prefix [String] that should be matched with tag prefix
     * @param page [Pageable]
     * @return [List] of [Tag]s with [Tag.name] that starts with [prefix]
     */
    fun getVulnerabilityTagsByPrefix(
        prefix: String,
        page: Pageable,
    ) = lnkCosvMetadataTagRepository.findAllByTagNameStartingWith(prefix, page).map { it.tag }
}
