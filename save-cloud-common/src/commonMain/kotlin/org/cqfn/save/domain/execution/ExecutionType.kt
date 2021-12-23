package org.cqfn.save.domain.execution

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
