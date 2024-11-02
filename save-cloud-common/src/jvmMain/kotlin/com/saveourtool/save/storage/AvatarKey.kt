package com.saveourtool.save.storage

import com.saveourtool.save.utils.AvatarType

/**
 * @property type
 * @property objectName
 */
data class AvatarKey(
    val type: AvatarType,
    val objectName: String,
)
