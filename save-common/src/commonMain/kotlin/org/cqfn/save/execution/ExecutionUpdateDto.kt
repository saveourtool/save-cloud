package org.cqfn.save.execution

class ExecutionUpdateDto (
    val id: Long,
    val status: ExecutionStatus
) {
    constructor(): this(0, ExecutionStatus.PENDING)
}
