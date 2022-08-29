package com.saveourtool.save.domain

/**
 * A common enum of statuses for saving entity
 */
enum class SaveEntityStatus {
    /**
     * Conflict while saving entity
     */
    CONFLICT,

    /**
     * Entity exists already
     */
    EXIST,

    /**
     * New entity saved successfully
     */
    NEW,
    ;
}
