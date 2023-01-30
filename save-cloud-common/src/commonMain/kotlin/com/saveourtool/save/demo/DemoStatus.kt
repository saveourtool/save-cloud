package com.saveourtool.save.demo

import kotlinx.serialization.Serializable

/**
 * Enum that defines demo status
 */
@Serializable
enum class DemoStatus {
    /**
     * Demo is created but stopped by error
     */
    ERROR,

    /**
     * No demo created yet
     */
    NOT_CREATED,

    /**
     * Demo is ready for use
     */
    RUNNING,

    /**
     * Demo is already created but not ready for use yet
     */
    STARTING,

    /**
     * Demo is created but stopped by owner
     */
    STOPPED,
    ;
}
