package com.saveourtool.save.cosv.storage

import kotlinx.datetime.LocalDateTime

/**
 * @property id
 * @property modified
 * @property isValid
 */
data class CosvKey(
    val id: String,
    val modified: LocalDateTime,
    val isValid: Boolean = true,
)
