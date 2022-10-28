package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * Enum of organization status
 */
@Serializable
enum class OrganizationStatus {
    /**
     * The organization is banned by a super admin
     */
    BANNED,

    /**
     * Organization created
     */
    CREATED,

    /**
     * Organization deleted
     */
    DELETED,
    ;
}
