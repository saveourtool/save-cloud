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
abstract class BaseEntityWithDate : BaseEntity() {
    /**
     * Create date of entity
     **/
    var createDate: LocalDateTime? = null

    /**
     * Update date of entity
     **/
    var updateDate: LocalDateTime? = null
}
