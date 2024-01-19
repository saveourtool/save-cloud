package com.saveourtool.save.cosv.service

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.cosv.repositorysave.CommentRepository
import com.saveourtool.save.cosv.repositorysave.UserRepository
import com.saveourtool.save.entities.Comment
import com.saveourtool.save.entities.CommentDto
import com.saveourtool.save.evententities.CommentEvent
import com.saveourtool.save.utils.getByIdOrNotFound

import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import kotlinx.datetime.toJavaLocalDateTime

/**
 * Service for comments
 */
@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    /**
     * @param comment comment of user
     * @param authentication
     */
    @Transactional
    fun saveComment(comment: CommentDto, authentication: Authentication) {
        val userId = authentication.userId()
        val user = userRepository.getByIdOrNotFound(userId)

        val newComment = Comment(
            comment.message,
            comment.section,
            user,
        )

        applicationEventPublisher.publishEvent(CommentEvent(newComment))

        commentRepository.save(newComment)
    }

    /**
     * @param section section with comments
     * @return list of messages
     */
    fun findAllBySection(section: String) = commentRepository.getAllBySection(section)

    /**
     * @param section section with comments
     * @return count messages
     */
    fun countBySection(section: String) = commentRepository.countAllBySection(section)

    /**
     * @param comment [CommentDto] that matches the [Comment] that should be deleted
     * @return [Unit] if comment was found, `null` otherwise
     */
    @Transactional
    fun deleteComment(comment: CommentDto): Unit? {
        val requestedComment = commentRepository.findByUserNameAndSectionAndCreateDate(
            comment.userName,
            comment.section,
            comment.createDate?.toJavaLocalDateTime()
        )

        return requestedComment?.let { commentRepository.delete(it) }
    }
}
