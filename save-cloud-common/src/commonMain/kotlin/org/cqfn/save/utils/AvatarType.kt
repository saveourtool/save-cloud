package org.cqfn.save.utils

import kotlinx.serialization.Serializable

/**
 * Types of avatar
 */
@Serializable
enum class AvatarType {
    /**
     * Organization avatar
     */
    ORGANIZATION,

    /**
     * User avatar
     */
    USER,
    ;
}
