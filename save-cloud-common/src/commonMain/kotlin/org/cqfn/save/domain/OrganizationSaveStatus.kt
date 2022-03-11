package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of organization save status
 * @property message
 */
@Serializable
@Suppress("CUSTOM_GETTERS_SETTERS")
enum class OrganizationSaveStatus(val message: String) {
    /**
     * Organization exists
     */
    EXIST("Organization already exists"),

    /**
     * New organization
     */
    NEW("Organization saved successfully"),
    ;
}
