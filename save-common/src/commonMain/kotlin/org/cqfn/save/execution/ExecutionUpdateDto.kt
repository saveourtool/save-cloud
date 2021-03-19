package org.cqfn.save.execution

/**
 * @property id
 * @property status
 */
data class ExecutionUpdateDto(
    val id: Long,
    val status: ExecutionStatus
) {
    constructor() : this(0, ExecutionStatus.PENDING)
}
