package com.saveourtool.save.authservice.utils

/**
 * @property id
 * @property identitySource
 */
data class AuthenticationDetails(
    val id: Long,
    val identitySource: String? = null,
)
