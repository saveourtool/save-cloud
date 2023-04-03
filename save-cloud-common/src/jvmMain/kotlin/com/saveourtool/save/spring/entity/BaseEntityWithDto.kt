package com.saveourtool.save.spring.entity

/**
 * base class for all entities with DTO with [D] type
 */
abstract class BaseEntityWithDto<D : Any> : BaseEntity() {
    /**
     * @return DTO [D] for current entity
     */
    abstract fun toDto(): D
}
