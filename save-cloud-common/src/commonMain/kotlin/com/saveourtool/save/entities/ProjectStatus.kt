package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * Enum of project status
 */
@Serializable
enum class ProjectStatus {
    /**
     * Project created
     */
    CREATED,

    /**
     * Project deleted
     */
    DELETED,

    /**
     * The project is banned by a super admin
     */
    BANNED,
    ;
}
