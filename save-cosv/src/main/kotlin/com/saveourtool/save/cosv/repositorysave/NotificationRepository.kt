package com.saveourtool.save.cosv.repositorysave

import com.saveourtool.save.entities.Notification
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * The repository of notification entities.
 */
@Repository
interface NotificationRepository : BaseEntityRepository<Notification> {
    /**
     * @param name
     * @return list of notification
     */
    fun findByUserName(name: String): List<Notification>
}
