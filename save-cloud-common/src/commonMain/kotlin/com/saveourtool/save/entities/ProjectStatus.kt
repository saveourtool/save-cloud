package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * Enum of project status
 */
@Serializable
enum class ProjectStatus {
    /**
     * Project deleted
     */
    BANNED,

    /**
     * Project created
     */
    CREATED,

    /**
     * Project deleted
     */
    DELETED,
    ;
}
