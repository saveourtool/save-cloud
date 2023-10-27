package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntityWithDateAndDto

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property message
 * @property user
 */
@Entity
class Notification(
    var message: String,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,
) : BaseEntityWithDateAndDto<NotificationDto>() {
    override fun toDto() = NotificationDto(
        id = requiredId(),
        message = message,
        createDate = createDate?.toKotlinLocalDateTime(),
    )
}
