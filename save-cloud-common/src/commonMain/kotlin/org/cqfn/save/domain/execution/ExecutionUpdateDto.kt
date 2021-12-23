package org.cqfn.save.domain.execution

import kotlinx.serialization.Serializable

/**
 * @property id
 * @property status
 */
@Serializable
class ExecutionUpdateDto(
    val id: Long,
    val status: ExecutionStatus,
)
