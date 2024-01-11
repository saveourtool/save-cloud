package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.Notification
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * The repository of notification entities.
 */
@Repository
interface NotificationRepository {
    /**
     * @param message message of notification
     * @param userId id of user
     * @return save tag
     */
    @Query(
        value = "insert into save_cloud.notification (message, user_id) values (:message, :userId)",
        nativeQuery = true,
    )
    fun saveNotification(
        @Param("message") message: String,
        @Param("userId") userId: Long,
    ): Notification
}
