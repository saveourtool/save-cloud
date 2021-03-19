package org.cqfn.save.execution

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property status
 */
@Serializable
class ExecutionUpdateDto(
    val id: Long,
    val status: ExecutionStatus
)
