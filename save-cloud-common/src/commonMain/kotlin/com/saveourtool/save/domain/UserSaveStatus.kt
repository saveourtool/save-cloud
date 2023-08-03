package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of user save status
 * @property message
 */
@Serializable
enum class UserSaveStatus(val message: String) {
    /**
     * User has conflicting name
     */
    CONFLICT("This name is already taken"),

    /**
     * User name longer than 22 characters
     */
    INVALID_NAME("Name must not be longer than 22 characters"),

    /**
     * Deleted user
     */
    DELETED("User successfully deleted"),

    /**
     * New user
     */
    NEW("User saved successfully"),

    /**
     * Update user
     */
    UPDATE("User successfully updated"),
    ;
}
