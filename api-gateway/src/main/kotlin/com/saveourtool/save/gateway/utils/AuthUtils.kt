/**
 * Utility methods to work with authentication-related objects
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.gateway.utils

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import java.security.Principal

/**
 * @property identitySource
 * @property id
 */
// TODO: Temp copy-paste solution to avoid dependency on authservice
class IdentitySourceAwareUserDetails(
    username: String,
    password: String?,
    authorities: String?,
    val identitySource: String,
    val id: Long,
) : User(
    username,
    password,
    authorities?.split(',')
        ?.filter { it.isNotBlank() }
        ?.map { SimpleGrantedAuthority(it) }
        .orEmpty()
)

/**
 * @return username extracted from this [Principal]
 */
fun Principal.userName(): String = when (this) {
    is OAuth2AuthenticationToken -> (this as? OAuth2AuthenticationToken)
        ?.principal
        ?.name
        ?: this.name
    else -> this.name
}

/**
 * @return string representation of source of this [Authentication]
 */
fun Authentication.toIdentitySource(): String = when (this) {
    is OAuth2AuthenticationToken -> authorizedClientRegistrationId
    // FixMe: for now this method only used for OAuth2 login, if it will be used for
    // FixMe: basic authorization too, it should provide proper source somehow
    is UsernamePasswordAuthenticationToken -> "basic"
    else -> this.javaClass.simpleName
}
