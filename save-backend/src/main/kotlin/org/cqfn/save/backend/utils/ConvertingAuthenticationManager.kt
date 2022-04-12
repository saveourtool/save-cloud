package org.cqfn.save.backend.utils

import org.cqfn.save.backend.service.UserDetailsService
import org.cqfn.save.utils.IdentitySourceAwareUserDetails
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
        println("\n\n-----------------Backend, authenticate: ${authentication.name} ${authentication.isAuthenticated}")
        val identitySource = (authentication.details as AuthenticationDetails).identitySource
        if (identitySource == null || !authentication.name.startsWith("$identitySource:")) {
            throw BadCredentialsException(authentication.name)
        }
        val name = authentication.name.drop(identitySource.length + 1)
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
                it.toAuthenticationWithDetails(authentication)
            }
    } else {
        println("\n\nUnsupported authentication type ${authentication::class}")
        Mono.error { BadCredentialsException("Unsupported authentication type ${authentication::class}") }
    }

    /**
     * Fixme: since identitySource is in `AuthenticationDetails`, it can be removed from principal
     */
    private fun IdentitySourceAwareUserDetails.toAuthenticationWithDetails(authentication: Authentication) =
            UsernamePasswordAuthenticationToken(
                "$identitySource:$username",
                authentication.credentials,
                authorities
            ).apply {
                details = AuthenticationDetails(
                    id = id,
                    identitySource = identitySource,
                )
            }
}
