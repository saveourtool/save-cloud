package org.cqfn.save.execution

import kotlinx.serialization.Serializable

/**
 * Type oj execution
 */
@Serializable
enum class ExecutionType {
    /**
     * project from git
     */
    MANUAL,

    /**
     * Project by binary file
     */
    STANDARD,
    ;
}
