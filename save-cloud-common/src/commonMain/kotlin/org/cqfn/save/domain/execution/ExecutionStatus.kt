package org.cqfn.save.domain.execution

import kotlinx.serialization.Serializable

/**
 * Statuses of execution
 */
@Serializable
enum class ExecutionStatus {
    /**
     * Execution has completed abnormally with error on the side of save-cloud
     */
    ERROR,

    /**
     * Execution has completed successfully (some tests might have failed, but there have been no internal errors)
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
