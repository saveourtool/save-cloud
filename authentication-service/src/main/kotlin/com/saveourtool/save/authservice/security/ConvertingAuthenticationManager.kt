package com.saveourtool.save.authservice.security

import com.saveourtool.save.authservice.service.AuthenticationUserDetailsService
import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.authservice.utils.IdentitySourceAwareUserDetails
import com.saveourtool.save.authservice.utils.extractUserNameAndIdentitySource

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Implementation of `ReactiveAuthenticationManager` that doesn't check user credentials
 * and simply authenticates every user with username. Should be used in secure environment,
 * where user identity is already guaranteed.
 */
@Component
class ConvertingAuthenticationManager(
    @Autowired private var authenticationUserDetailsService: AuthenticationUserDetailsService
) : ReactiveAuthenticationManager {
    /**
     * Authenticate user, by checking the received data, which converted into UsernamePasswordAuthenticationToken
     * by [CustomAuthenticationBasicConverter] with record in DB
     *
     * @return augmented mono of UsernamePasswordAuthenticationToken with additional details
     * @throws BadCredentialsException in case of bad credentials
     */
    override fun authenticate(authentication: Authentication): Mono<Authentication> = if (authentication is UsernamePasswordAuthenticationToken) {
        val (name, identitySource) = authentication.extractUserNameAndIdentitySource()
        val nameAndSource = "$name$AUTH_SEPARATOR$identitySource"
        authenticationUserDetailsService.findByUsername(nameAndSource)
            .cast<IdentitySourceAwareUserDetails>()
            .filter {
                it.identitySource == identitySource
            }
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
        Mono.error { BadCredentialsException("Unsupported authentication type ${authentication::class}") }
    }

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

    companion object Factory {
        const val AUTH_SEPARATOR = "@SAVE@"
    }
}
