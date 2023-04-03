package com.saveourtool.save.spring.entity

import com.saveourtool.save.validation.Validatable
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

/**
 * base class for all entities
 */
@MappedSuperclass
@Suppress("CLASS_SHOULD_NOT_BE_ABSTRACT", "UnnecessaryAbstractClass")
abstract class BaseEntity : Validatable {
    /**
     * generate a unique id
     */
    @Id
    @GeneratedValue
    open var id: Long? = null

    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    open fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
