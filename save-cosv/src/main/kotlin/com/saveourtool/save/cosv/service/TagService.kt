package com.saveourtool.save.cosv.service

import com.saveourtool.save.cosv.repository.TagRepository
import com.saveourtool.save.entities.Tag

/**
 * Service for tag
 */
class TagService(
    private val tagRepository: TagRepository,
) {
    /**
     * @param name name of tag
     */
    fun saveTag(name: String) = tagRepository.saveTag(name)

    /**
     * @param name
     * @return tag with [name]
     */
    fun findTagByName(name: String): Tag? = tagRepository.findByName(name)
}
