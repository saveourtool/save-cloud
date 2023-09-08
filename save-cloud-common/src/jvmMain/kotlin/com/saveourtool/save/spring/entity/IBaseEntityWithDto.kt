package com.saveourtool.save.spring.entity

/**
 * base class for all entities with DTO with [D] type
 */
interface IBaseEntityWithDto<D : Any> {
    /**
     * @return DTO [D] for current entity
     */
    fun toDto(): D
}