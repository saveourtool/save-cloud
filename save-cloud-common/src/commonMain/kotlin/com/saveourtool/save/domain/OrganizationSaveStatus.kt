package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of organization save status
 * @property message
 */
@Serializable
enum class OrganizationSaveStatus(val message: String) {
    /**
     * Organization has conflicting name
     */
    CONFLICT("Organization name is already taken"),

    /**
     * New organization
     */
    NEW("Organization saved successfully"),
    ;
}
