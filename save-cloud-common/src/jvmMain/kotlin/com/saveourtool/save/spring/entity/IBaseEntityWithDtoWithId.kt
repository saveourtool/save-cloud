package com.saveourtool.save.spring.entity

import com.saveourtool.save.entities.DtoWithId

/**
 * base class for all entities with DTO with [D] type where DTO has ID
 */
interface IBaseEntityWithDtoWithId<D : DtoWithId> : IBaseEntityWithDto<D>
