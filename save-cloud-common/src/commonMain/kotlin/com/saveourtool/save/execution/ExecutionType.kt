package com.saveourtool.save.execution

import kotlinx.serialization.Serializable

/**
 * Type oj execution
 */
@Serializable
enum class ExecutionType {
    /**
     * Project on contest
     */
    CONTEST,

    /**
     * Project from git
     */
    GIT,

    /**
     * Project by binary file
     */
    STANDARD,
    ;
}
