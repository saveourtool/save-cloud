package com.saveourtool.save.authservice.security

import com.saveourtool.save.authservice.repository.AuthenticationUserRepository
import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.authservice.utils.username
import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.blockingToMono

import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Implementation of `ReactiveAuthenticationManager` that doesn't check user credentials
 * and simply authenticates every user with username. Should be used in secure environment,
 * where user identity is already guaranteed.
 */
@Component
class ConvertingAuthenticationManager(
    private val authenticationUserRepository: AuthenticationUserRepository,
) : ReactiveAuthenticationManager {
    /**
     * Authenticate user, by checking the received data
     *
     * @return augmented mono of UsernamePasswordAuthenticationToken with additional details
     * @throws BadCredentialsException in case of bad credentials
     */
    override fun authenticate(authentication: Authentication): Mono<Authentication> = if (authentication is UsernamePasswordAuthenticationToken) {
        val name = authentication.username()
        blockingToMono {
            authenticationUserRepository.findByName(name)
        }
            .switchIfEmpty {
                Mono.error { BadCredentialsException(name) }
            }
            .onErrorMap {
                BadCredentialsException(name)
            }
            .map {
                it.toAuthenticationWithDetails()
            }
    } else {
        Mono.error { BadCredentialsException("Unsupported authentication type ${authentication::class}") }
    }

    private fun User.toAuthenticationWithDetails() =
        UsernamePasswordAuthenticationToken(
            name,
            password,
            AuthorityUtils.commaSeparatedStringToAuthorityList(role)
        ).apply {
            details = AuthenticationDetails(
                id = requiredId(),
            )
        }
}
