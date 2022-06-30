package com.saveourtool.save.testsuite

import com.saveourtool.save.utils.Secret

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data class (DTO) with git repository information for entry [com.saveourtool.save.entities.GithubLocation]
 * @property httpUrl
 * @property username
 * @property token it can be null for public repositories
 * @property branch a specific branch name with test, otherwise default branch (main or master)
 * @property subDirectory a path to test suites in repository, otherwise root directory
 */
@Serializable
data class GitLocation(
    val httpUrl: String,
    val username: String?,
    @Secret val token: String?,
    var branch: String,
    var subDirectory: String,
) : TestSuitesSourceLocation {
    init {
        require((username == null && token == null) || (username != null && token != null)) {
            "Public repository: username and token are not provided; " +
                    "Private repository: username and token are provided and not null"
        }
    }

    override fun getType(): TestSuitesSourceLocationType = TestSuitesSourceLocationType.GIT

    /**
     * @return formatted String to store it in database
     */
    override fun formatForDatabase(): String = Json.encodeToString(this)

    /**
     * @return default name fot TestSuitesSourceName
     */
    fun defaultTestSuitesSourceName() = buildString {
        append("Git[$httpUrl]")
        append("/$subDirectory")
        append(":$branch")
    }

    companion object : TestSuitesSourceLocation.Companion<GitLocation> {
        /**
         * @param databaseValue
         * @return parsed value taken from database
         */
        override fun parseFromDatabase(databaseValue: String): GitLocation = Json.decodeFromString(databaseValue)
    }
}
