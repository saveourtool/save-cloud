package org.cqfn.save.repository

import kotlinx.serialization.Serializable

/**
 * Data class with repository information
 * fixme should operate not with password, but with some sort of token (github integration)
 *
 * @property url - url of repo
 * @property username - username to credential
 * @property password - password to credential
 * @property branch - branch to clone
 */
@Serializable
data class GitRepository(
        val url: String,
        val username: String? = null,
        val password: String? = null,
        val branch: String? = null,
)