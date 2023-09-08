package com.saveourtool.save.spring.entity

import com.saveourtool.save.listeners.DateListener
import java.time.LocalDateTime
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass

/**
 * base interface for all entities with [createDate] and [updateDate]
 */
@MappedSuperclass
@EntityListeners(DateListener::class)
interface IBaseEntityWithDate {
    /**
     * Create date of entity
     **/
    var createDate: LocalDateTime?

    /**
     * @return [createDate] as not null with validating
     * @throws IllegalArgumentException when [createDate] is not set that means entity is not saved yet
     */
    fun requiredCreateDate(): LocalDateTime = requireNotNull(createDate) {
        "Entity is not saved yet: $this"
    }

    /**
     * Update date of entity
     **/
    var updateDate: LocalDateTime?

    /**
     * @return [updateDate] as not null with validating
     * @throws IllegalArgumentException when [updateDate] is not set that means entity is not saved yet
     */
    fun requiredUpdateDate(): LocalDateTime = requireNotNull(updateDate) {
        "Entity is not saved yet: $this"
    }
}
