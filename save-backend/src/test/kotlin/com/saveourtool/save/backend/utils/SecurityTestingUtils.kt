/**
 * Utilities for testing of security-related logic
 */

package com.saveourtool.save.backend.utils

import com.saveourtool.save.authservice.utils.AuthenticationUserDetails
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * @param id user.id for a mocked user in test security context
 */
internal fun mutateMockedUser(id: Long) {
    SecurityContextHolder.getContext().apply {
        authentication = AuthenticationUserDetails(
            id,
            authentication.name,
            (authentication as UsernamePasswordAuthenticationToken).authorities.joinToString(",") { it.authority }
        ).toAuthenticationToken()
    }
}
