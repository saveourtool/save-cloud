package com.saveourtool.save.api.authorization

/**
 * Authorization data
 *
 * @property userInformation user name
 * @property token
 */
data class Authorization(
    val userInformation: String,
    val token: String?,
)
