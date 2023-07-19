/**
 * Utility methods to work with authentication-related objects
 */

package com.saveourtool.save.gateway.utils

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import java.security.Principal

/**
 * @return username extracted from this [Principal]
 */
fun Principal.userName(): String = when (this) {
    is OAuth2AuthenticationToken -> this.principal.name
    else -> this.name
}
