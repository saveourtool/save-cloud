package org.cqfn.save.backend.utils

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User

/**
 * @param authorities comma-separated authorities as a single string
 * @property identitySource
 */
class IdentitySourceAwareUserDetails(
    username: String,
    password: String?,
    authorities: String?,
    val identitySource: String,
) : User(
    username,
    password,
    authorities?.split(',')
        ?.filter { it.isNotBlank() }
        ?.map { SimpleGrantedAuthority(it) }
        ?: emptyList()
)
