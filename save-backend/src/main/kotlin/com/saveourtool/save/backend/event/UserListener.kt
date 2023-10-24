package com.saveourtool.save.backend.event

import com.saveourtool.save.backend.service.NotificationService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.LnkUserNotification
import com.saveourtool.save.entities.Notification
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserStatus
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * A user listener for sending notifications.
 */
@Component
class UserListener(
    private val userDetailsService: UserDetailsService,
    private val notificationService: NotificationService,
) {
    /**
     * @param user new user
     */
    @EventListener
    fun createUser(user: User) {
        if (user.status == UserStatus.NOT_APPROVED) {
            val recipients = userDetailsService.findByRole(Role.SUPER_ADMIN.asSpringSecurityRole())
            val newNotification = notificationService.save(Notification(message = messageNewUser(user)))
            val lnkUserNotifications = recipients.map {
                LnkUserNotification(
                    notification = newNotification,
                    user = it,
                    isShow = false,
                )
            }
            notificationService.saveAllLnkUserNotification(lnkUserNotifications)
        }
    }

    companion object {
        /**
         * @param user
         * @return message
         */
        fun messageNewUser(user: User) = """
            New user: ${user.name} is waiting for approve of his account.
        """.trimIndent()
    }
}
