package com.saveourtool.save.backend.storage

import com.saveourtool.save.utils.AvatarType

/**
 * @property type
 * @property objectName
 */
data class AvatarKey(
    val type: AvatarType,
    val objectName: String,
) {
    /**
     * Added for backward compatibility
     *
     * @return relative path to avatar image
     */
    fun getRelativePath(): String = "/${type.folder}/$objectName"
}
