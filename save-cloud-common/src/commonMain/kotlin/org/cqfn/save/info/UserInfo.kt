package org.cqfn.save.info

import kotlinx.serialization.Serializable

/**
 * Represents all data related to the User
 *
 * @property userName name/login of the user
 * @property avatar avatar of user
 */
@Serializable
data class UserInfo(
    val userName: String,
    val avatar: String? = null,
)
