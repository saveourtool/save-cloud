package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Tag
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * [BaseEntityRepository] for [Tag]s.
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
     * Find [Tag]s by their [Tag.name]
     *
     * @param tagNames [Set] of [Tag.name]
     * @return [Set] of [Tag]
     */
    fun findByNameIn(tagNames: Set<String>): Set<Tag>
}
