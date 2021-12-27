package org.cqfn.save.backend.utils

import org.cqfn.save.backend.service.UserDetailsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

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
        val identitySource = (authentication.details as Map<*, *>)["identitySource"] as String?
        if (!authentication.name.startsWith("$identitySource:")) {
            throw BadCredentialsException(authentication.name)
        }
        val name = identitySource?.let { authentication.name.drop(it.length + 1) } ?: authentication.name
        userDetailsService.findByUsername(name)
            .map { it as IdentitySourceAwareUserDetails }
            .filter { it.identitySource == identitySource }
            .switchIfEmpty {
                Mono.error { BadCredentialsException(name) }
            }
            .onErrorMap {
                BadCredentialsException(name)
            }
            .map {
                UsernamePasswordAuthenticationToken(
                    "$identitySource:${it.username}",
                    authentication.credentials,
                    it.authorities
                )
            }
    } else {
        Mono.error { BadCredentialsException("Unsupported authentication type ${authentication::class}") }
    }
}
