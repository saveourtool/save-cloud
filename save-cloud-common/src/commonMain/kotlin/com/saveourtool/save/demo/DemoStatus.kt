package com.saveourtool.save.demo

/**
 * Enum that defines demo status
 */
enum class DemoStatus {
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
     * Demo is created but stopped by owner/error
     */
    STOPPED,
    ;
}
