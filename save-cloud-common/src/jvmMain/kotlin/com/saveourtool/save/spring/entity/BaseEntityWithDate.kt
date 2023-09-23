package com.saveourtool.save.spring.entity

import com.saveourtool.save.listeners.DateListener
import java.time.LocalDateTime
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass

/**
 * base class for all entities with [createDate] and [updateDate]
 */
@MappedSuperclass
@EntityListeners(DateListener::class)
@Suppress("UnnecessaryAbstractClass")
abstract class BaseEntityWithDate : BaseEntity(), IBaseEntityWithDate {
    /**
     * Create date of entity
     **/
    override var createDate: LocalDateTime? = null

    /**
     * Update date of entity
     **/
    override var updateDate: LocalDateTime? = null
}
