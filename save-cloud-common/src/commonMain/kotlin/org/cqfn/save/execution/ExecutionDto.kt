package org.cqfn.save.execution

import kotlinx.serialization.Serializable

/**
 * @property version
 * @property status
 * @property type
 */
@Serializable
class ExecutionDto(
    val status: ExecutionStatus,
    val type: ExecutionType,
    val version: String,
)
