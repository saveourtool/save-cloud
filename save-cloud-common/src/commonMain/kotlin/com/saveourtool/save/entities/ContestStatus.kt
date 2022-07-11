package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * Enum of contest status
 */
@Serializable
enum class ContestStatus {
    /**
     * Created contest
     */
    CREATED,

    /**
     * Deleted contest
     */
    DELETED,
    ;
}
