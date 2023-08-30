package com.saveourtool.save.domain

import com.saveourtool.save.validation.NAMING_MAX_LENGTH
import kotlinx.serialization.Serializable

/**
 * Enum of user save status
 * @property message
 */
@Serializable
enum class UserSaveStatus(val message: String) {
    /**
     * Approved user
     */
    APPROVED("User successfully approved"),

    /**
     * Banned user
     */
    BANNED("User successfully banned"),

    /**
     * User has conflicting name
     */
    CONFLICT("This name is already taken"),

    /**
     * Deleted user
     */
    DELETED("User successfully deleted"),

    /**
     * currentUser.id != changedUser.id
     */
    HACKER("You are trying to update other user that is not you"),

    /**
     * User name longer than [NAMING_MAX_LENGTH] characters
     */
    INVALID_NAME("Name must not be longer than $NAMING_MAX_LENGTH characters"),

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
