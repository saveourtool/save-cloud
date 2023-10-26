package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Notification
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository to access data about user notification
 */
@Repository
interface NotificationRepository : BaseEntityRepository<Notification>
