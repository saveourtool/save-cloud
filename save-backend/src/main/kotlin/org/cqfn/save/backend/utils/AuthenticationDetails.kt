package com.saveourtool.save.backend.utils

/**
 * @property id
 * @property identitySource
 */
data class AuthenticationDetails(
    val id: Long,
    val identitySource: String? = null,
)
