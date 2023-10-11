package com.saveourtool.save.entities.cosv

import kotlinx.serialization.Serializable

/**
 * @property result
 * @property processedSize
 * @property fullSize
 */
@Serializable
data class UnzipRawCosvFileResponse(
    val result: RawCosvFileDto?,
    val processedSize: Long,
    val fullSize: Long,
)
