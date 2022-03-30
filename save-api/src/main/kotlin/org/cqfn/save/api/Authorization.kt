package org.cqfn.save.api

/**
 * Authorization data
 *
 * @property userName
 * @property password
 */
data class Authorization(
    val userName: String,
    val password: String? = null
)
