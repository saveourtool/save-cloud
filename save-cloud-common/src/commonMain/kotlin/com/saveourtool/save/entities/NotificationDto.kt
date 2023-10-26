package com.saveourtool.save.entities

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property message
 * @property createDate
 */
@Serializable
class NotificationDto(
    val message: String,
    val createDate: LocalDateTime?,
)
