package com.saveourtool.save.entities

/**
 * base class for all dtos
 */
@Suppress("CLASS_SHOULD_NOT_BE_ABSTRACT", "UnnecessaryAbstractClass")
abstract class BaseDto {
    /**
     * unique id
     */
    abstract val id: Long?

    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    open fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
