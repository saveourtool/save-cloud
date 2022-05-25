package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of organization save status
 * @property message
 */
@Serializable
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
