package com.saveourtool.save.utils

import kotlinx.serialization.Serializable

/**
 * Types of avatar
 * @property folder
 */
@Serializable
enum class AvatarType(val folder: String) {
    /**
     * Organization avatar
     */
    ORGANIZATION("organizations"),

    /**
     * User avatar
     */
    USER("users"),
    ;

    companion object {
        /**
         * @param folderCandidate
         * @return [AvatarType] found by provided [folder] or null
         */
        fun findByFolder(folderCandidate: String): AvatarType? = values().singleOrNull { it.folder == folderCandidate }
    }
}
