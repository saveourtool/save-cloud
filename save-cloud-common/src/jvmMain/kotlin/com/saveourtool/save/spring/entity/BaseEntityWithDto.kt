package com.saveourtool.save.spring.entity

import com.saveourtool.save.entities.DtoWithId

/**
 * base class for all entities with DTO with [D] type
 */
abstract class BaseEntityWithDto<D : Any> : BaseEntity() {
    /**
     * @return DTO [D] for current entity
     */
    abstract fun toDto(): D

    /**
     * @return DTO with ID [DtoWithId] with content [D] for current entity
     * @throws IllegalArgumentException when entity is not saved yet
     */
    open fun toDtoWithId(): DtoWithId<D> = DtoWithId(requiredId(), toDto())
}
