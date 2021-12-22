package org.cqfn.save.backend.utils

import org.cqfn.save.backend.service.UserDetailsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Implementation of `ReactiveAuthenticationManager` that doesn't check user credentials
 * and simply authenticates every user with username. Should be used in secure environment,
 * where user identity is already guaranteed.
 */
@Component
class ConvertingAuthenticationManager : ReactiveAuthenticationManager {
    @Autowired
    private lateinit var userDetailsService: UserDetailsService

    override fun authenticate(authentication: Authentication): Mono<Authentication> = if (authentication is UsernamePasswordAuthenticationToken) {
        userDetailsService.findByUsername(authentication.name).map {
            UsernamePasswordAuthenticationToken(it.username, null, it.authorities)
        }
    } else {
        Mono.error { IllegalStateException("Unsupported authentication type ${authentication::class}") }
    }
}
