package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property notification
 * @property user
 * @property isShow
 */
@Entity
class LnkUserNotification(

    @ManyToOne
    @JoinColumn(name = "notification_id")
    var notification: Notification,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    var isShow: Boolean,

) : BaseEntity()
