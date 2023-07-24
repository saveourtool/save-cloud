package com.saveourtool.save.info

import kotlinx.serialization.Serializable

/**
 * Enum of user status
 *
 * The order of the elements is used for sorting
 */
@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class UserStatus {
    /**
     * User created
     */
    CREATED,

    /**
     * User active
     */
    ACTIVE,

    /**
     * User deleted
     */
    DELETED,
    ;
}
