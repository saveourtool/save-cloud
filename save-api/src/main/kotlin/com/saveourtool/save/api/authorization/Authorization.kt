package com.saveourtool.save.api.authorization

/**
 * Authorization data
 *
 * @property userName user name
 * @property source user source
 * @property token
 */
data class Authorization(
    val userName: String,
    val source: String,
    val token: String?,
)
