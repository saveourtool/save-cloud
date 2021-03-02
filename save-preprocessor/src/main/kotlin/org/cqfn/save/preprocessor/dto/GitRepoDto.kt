package org.cqfn.save.preprocessor.dto

import kotlinx.serialization.Serializable

/**
 * Data class with repository information
 *
 * @param url - url of repo
 * @param username - username to credential
 * @param password - password to credential
 * @param branch - branch to clone
 */
@Serializable
data class GitRepoDto(
        val url: String,
        val username: String?,
        val password: String?,
        val branch: String?,
)