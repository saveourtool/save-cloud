package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * @property path path to image
 */
@Serializable
data class ImageInfo(
    val path: String?,
)
