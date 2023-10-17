package com.saveourtool.save.entities.cosv

import kotlinx.serialization.Serializable

/**
 * @property processedSize
 * @property fullSize
 * @property result
 * @property updateCounters
 */
@Serializable
data class RawCosvFileStreamingResponse(
    val processedSize: Long,
    val fullSize: Long,
    val result: List<RawCosvFileDto>? = null,
    val updateCounters: Boolean = false,
)
