package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.LnkUserNotificationRepository
import com.saveourtool.save.backend.repository.NotificationRepository
import com.saveourtool.save.entities.LnkUserNotification
import com.saveourtool.save.entities.Notification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for notifications
 */
@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val lnkUserNotificationRepository: LnkUserNotificationRepository,
) {
    /**
     * @param notification
     * @return saved notification
     */
    @Transactional
    fun save(notification: Notification): Notification = notificationRepository.saveAndFlush(notification)

    /**
     * @param lnkUserNotifications
     * @return saved lnkUserNotifications
     */
    @Transactional
    fun saveAllLnkUserNotification(lnkUserNotifications: List<LnkUserNotification>): List<LnkUserNotification> = lnkUserNotificationRepository.saveAll(lnkUserNotifications)
}
