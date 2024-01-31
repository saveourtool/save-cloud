package com.saveourtool.save.repository

import com.saveourtool.save.entities.Tag
import com.saveourtool.save.entitiescosv.LnkVulnerabilityMetadataTag
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * The repository of tag entities.
 */
@Repository
interface TagRepository : BaseEntityRepository<Tag> {
    /**
     * Find [Tag] by its [Tag.name]
     *
     * @param name tag name
     * @return [Tag] if found, null otherwise
     */
    fun findByName(name: String): Tag?

    /**
     * @param prefix [LnkVulnerabilityMetadataTag.tag] name prefix
     * @return [List] of [LnkVulnerabilityMetadataTag]s with name that starts with [prefix]
     */
    fun findAllByNameStartingWith(prefix: String): List<Tag>

    /**
     * @param ids tags id
     * @return [List] of [Tag]s
     */
    fun findAllByIdIn(ids: List<Long>): List<Tag>
}
