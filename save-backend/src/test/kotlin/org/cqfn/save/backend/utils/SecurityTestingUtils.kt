package org.cqfn.save.backend.utils

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * @param action
 * @return
 */
internal fun mutateMockedUser(action: UsernamePasswordAuthenticationToken.() -> Unit) =
        SecurityContextHolder.getContext().apply {
            authentication = (authentication as UsernamePasswordAuthenticationToken).apply(action)
        }
