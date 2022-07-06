package com.saveourtool.save.execution

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property status
 */
@Serializable
data class ExecutionUpdateDto(
    val id: Long,
    val status: ExecutionStatus,
    val failReason: String? = null
)
