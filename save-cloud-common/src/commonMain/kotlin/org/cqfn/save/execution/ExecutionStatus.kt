package org.cqfn.save.execution

import kotlinx.serialization.Serializable

/**
 * Statuses of execution
 */
@Serializable
enum class ExecutionStatus {
    /**
     * Status error
     */
    ERROR,

    /**
     * Status finished
     */
    FINISHED,

    /**
     * Status pending
     */
    PENDING,

    /**
     * Status running
     */
    RUNNING,
    ;
}
