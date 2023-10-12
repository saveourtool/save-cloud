package com.saveourtool.save.entities.cosv

import kotlinx.serialization.Serializable

/**
 * @property processedSize
 * @property fullSize
 * @property result
 * @property updateCounters
 */
@Serializable
data class UnzipRawCosvFileResponse(
    val processedSize: Long,
    val fullSize: Long,
    val result: RawCosvFileDto? = null,
    val updateCounters: Boolean = false,
)
