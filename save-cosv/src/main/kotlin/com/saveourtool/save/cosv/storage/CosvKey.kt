package com.saveourtool.save.cosv.storage

import kotlinx.datetime.LocalDateTime

/**
 * @property id
 * @property modified
 */
data class CosvKey(
    val id: String,
    val modified: LocalDateTime,
)
