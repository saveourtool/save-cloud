package com.saveourtool.save.osv.storage

import kotlinx.datetime.LocalDateTime

/**
 * @property id
 * @property modified
 */
data class OsvKey(
    val id: String,
    val modified: LocalDateTime,
)
