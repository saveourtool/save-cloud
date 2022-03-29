package org.cqfn.save.api

/**
 * @property userName
 * @property password
 */
data class Authorization(
    val userName: String,
    val password: String? = null
)
