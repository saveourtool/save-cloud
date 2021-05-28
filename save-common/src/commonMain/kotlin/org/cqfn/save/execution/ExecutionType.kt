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
    GIT,

    /**
     * Project by binary file
     */
    STANDARD,
    ;
}
