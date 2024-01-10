package com.saveourtool.save.cosv.event

import com.saveourtool.save.cosv.repository.LnkVulnerabilityMetadataUserRepository
import com.saveourtool.save.cosv.repository.NotificationRepository
import com.saveourtool.save.cosv.service.VulnerabilityMetadataService
import com.saveourtool.save.entities.User
import com.saveourtool.save.evententities.CommentEvent
import com.saveourtool.save.utils.orNotFound
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * A comment listener for sending notifications.
 */
@Component
class CommentListener(
    private val notificationRepository: NotificationRepository,
    private val vulnerabilityMetadataService: VulnerabilityMetadataService,
    private val lnkVulnerabilityMetadataUserRepository: LnkVulnerabilityMetadataUserRepository,
) {
    /**
     * @param commentEvent new commentEvent
     */
    @EventListener
    fun createComment(commentEvent: CommentEvent) {
        val sectionType = commentEvent.comment.section.substringAfter("/")
            .substringBefore("/")
        val id = commentEvent.comment.section.substringAfterLast("/")

        when (sectionType) {
            VULN -> createVulnerabilityComment(id, commentEvent.comment.user)
        }
    }

    private fun createVulnerabilityComment(identifier: String, commentOwner: User) {
        val vulnerability = vulnerabilityMetadataService.findByIdentifier(identifier).orNotFound { "Vulnerability with id: $identifier not found" }

        val recipients = lnkVulnerabilityMetadataUserRepository.findByVulnerabilityMetadataId(vulnerability.requiredId())
            .map { it.user }
            .plus(vulnerability.user)
            .minus(
                commentOwner
            )

        val message = messageNewVulnComment(commentOwner, identifier)
        recipients.map { user ->
            notificationRepository.saveNotification(message, user.requiredId())
        }
    }

    companion object {
        private const val VULN = "vuln"

        /**
         * @param user
         * @param identifier
         * @return message
         */
        fun messageNewVulnComment(user: User, identifier: String) = """
            User: ${user.name} added new comment in $identifier vulnerability.
        """.trimIndent()
    }
}
