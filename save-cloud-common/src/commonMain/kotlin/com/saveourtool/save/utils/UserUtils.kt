/**
 * Utilities of User entity
 */

package com.saveourtool.save.utils

import com.saveourtool.save.info.UserNameAndSource
import kotlinx.serialization.SerializationException

/**
 * @param userInformation
 * @return pair of username and source (where the user identity is coming from)
 */
fun extractUserNameAndSource(userInformation: String): Pair<String, String> = try {
    val userNameAndSource = UserNameAndSource.parse(userInformation)
    userNameAndSource.userName to userNameAndSource.source
} catch (_: SerializationException) {
    userInformation to "basic"
}
