package org.cqfn.save.api

/**
 * Authorization data
 *
 * @property userInformation user source and name, separated by `@`
 * @property token
 */
data class Authorization(
    val userInformation: String,
    val token: String? = null
)
