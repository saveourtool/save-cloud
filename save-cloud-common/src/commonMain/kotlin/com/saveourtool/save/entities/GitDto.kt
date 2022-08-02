package com.saveourtool.save.entities

import com.saveourtool.save.validation.Validatable
import com.saveourtool.save.validation.isValidUrl
import kotlinx.serialization.Serializable

/**
 * @property url
 * @property username
 * @property password
 */
@Serializable
data class GitDto(
    val url: String,
    val username: String? = null,
    val password: String? = null,
) : Validatable {
    override fun validate(): Boolean = url.isValidUrl()
}
