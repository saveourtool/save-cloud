/**
 * Utility methods to work with authentication-related objects
 */

package org.cqfn.save.gateway.utils

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import java.security.Principal

/**
 * @return username extracted from this [Principal]
 */
fun Principal.userName(): String  {
    return when (this) {
        is OAuth2AuthenticationToken -> (this as? OAuth2AuthenticationToken)
            ?.principal
            ?.name
            ?: this.name
        else -> this.name
    }
}

/**
 * @return string representation of source of this [Authentication]
 */
fun Authentication.toIdentitySource(): String = when (this) {
    is OAuth2AuthenticationToken -> authorizedClientRegistrationId
    is UsernamePasswordAuthenticationToken -> "${userName()}-basic"
    else -> this.javaClass.simpleName
}
