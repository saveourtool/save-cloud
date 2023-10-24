package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntityWithDateAndDto

import javax.persistence.Entity

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property message
 */
@Entity
class Notification(
    var message: String,
) : BaseEntityWithDateAndDto<NotificationDto>() {
    override fun toDto() = NotificationDto(
        message = message,
        createDate = createDate?.toKotlinLocalDateTime(),
    )
}
