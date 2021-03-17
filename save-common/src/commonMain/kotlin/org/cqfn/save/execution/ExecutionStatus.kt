package org.cqfn.save.execution

import kotlinx.serialization.Serializable

@Serializable
enum class ExecutionStatus {
    /**
     * Status pending
     */
    PENDING,

    /**
     * Status running
     */
    RUNNING,

    /**
     * Status finished
     */
    FINISHED,

    /**
     * Status error
     */
    ERROR,
}
