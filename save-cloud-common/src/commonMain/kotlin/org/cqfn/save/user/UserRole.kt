package org.cqfn.save.user

import kotlinx.serialization.Serializable

/**
 * Possible role users in project
 */
@Serializable
enum class UserRole {
    /**
     * Project admin
     */
    ADMIN,

    /**
     * Project owner
     */
    OWNER,

    /**
     * Read only
     */
    READ,

    /**
     * Project user
     */
    USER,
    ;
}
