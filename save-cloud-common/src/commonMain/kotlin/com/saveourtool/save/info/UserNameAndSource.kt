package com.saveourtool.save.info

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * @param userName
 * @param source
 */
@Serializable
data class UserNameAndSource(
    val userName: String,
    val source: String,
) {
    companion object {
        /**
         * @param userInformation
         * @return [UserNameAndSource] parsed from [userInformation]
         */
        fun parse(userInformation: String): UserNameAndSource = Json.decodeFromString(userInformation)
    }
}
