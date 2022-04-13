package org.cqfn.save.api

/**
 * Authorization data
 *
 * @property userName
 * @property token
 */
data class Authorization(
    val userName: String,
    val token: String? = null
)
