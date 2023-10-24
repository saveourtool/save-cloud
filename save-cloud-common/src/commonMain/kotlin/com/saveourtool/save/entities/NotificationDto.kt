package com.saveourtool.save.entities

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property message
 * @property createDate
 * @property isShow
 */
@Serializable
class NotificationDto(
    val message: String,
    val createDate: LocalDateTime?,
    val isShow: Boolean = false,
)
