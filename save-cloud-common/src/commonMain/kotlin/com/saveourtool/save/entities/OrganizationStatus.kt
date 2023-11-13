package com.saveourtool.save.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enum of organization status
 *
 * The order of the elements is used for sorting
 */
@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class OrganizationStatus {
    /**
     * Organization created
     */
    @SerialName("CREATED")
    CREATED,

    /**
     * Organization deleted
     */
    @SerialName("DELETED")
    DELETED,

    /**
     * Organization banned
     */
    @SerialName("BANNED")
    BANNED,
    ;
}
