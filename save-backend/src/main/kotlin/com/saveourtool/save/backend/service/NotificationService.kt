package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.NotificationRepository
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
) {
    /**
     * @param notification
     * @return saved notification
     */
    @Transactional
    fun save(notification: Notification): Notification = notificationRepository.save(notification)

    /**
     * @param notifications
     * @return saved notifications
     */
    @Transactional
    fun saveAll(notifications: List<Notification>): List<Notification> = notificationRepository.saveAll(notifications)
}
