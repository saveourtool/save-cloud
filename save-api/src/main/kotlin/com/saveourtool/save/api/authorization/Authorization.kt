package com.saveourtool.save.api.authorization

/**
 * Authorization data
 *
 * @property userName user name
 * @property token
 */
data class Authorization(
    val userName: String,
    val token: String?,
)
