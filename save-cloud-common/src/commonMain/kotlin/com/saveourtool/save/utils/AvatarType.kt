package com.saveourtool.save.utils

import kotlinx.serialization.Serializable

/**
 * Types of avatar
 */
@Serializable
enum class AvatarType {
    /**
     * default
     */
    NONE,

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
