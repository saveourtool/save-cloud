package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Comment
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository to access data about user comments
 */
@Repository
interface CommentRepository : BaseEntityRepository<Comment> {
    /**
     * @param section section of Save
     * @return list of user comments
     */
    fun getAllBySection(section: String): List<Comment>
}
