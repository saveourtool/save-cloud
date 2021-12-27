package org.cqfn.save.backend.utils

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

/**
 * @property identitySource
 */
class IdentitySourceAwareUserDetails(
    username: String,
    password: String?,
    authorities: Collection<GrantedAuthority>,
    val identitySource: String,
) : User(
    username,
    password,
    authorities
)
