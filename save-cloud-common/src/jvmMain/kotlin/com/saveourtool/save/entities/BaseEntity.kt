package com.saveourtool.save.entities

import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

/**
 * base class for all entities
 */
@MappedSuperclass
@Suppress("CLASS_SHOULD_NOT_BE_ABSTRACT", "UnnecessaryAbstractClass")
abstract class BaseEntity {
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
    @Suppress("CUSTOM_GETTERS_SETTERS")
    open val requiredId get(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
