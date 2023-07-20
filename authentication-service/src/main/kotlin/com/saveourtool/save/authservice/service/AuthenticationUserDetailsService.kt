package com.saveourtool.save.authservice.service

import com.saveourtool.save.authservice.repository.AuthenticationUserRepository
import com.saveourtool.save.authservice.utils.getIdentitySourceAwareUserDetails
import com.saveourtool.save.utils.AUTH_SEPARATOR
import com.saveourtool.save.utils.blockingToMono
import org.springframework.context.annotation.Primary
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * A service that provides `UserDetails`
 */
@Service
@Primary
class AuthenticationUserDetailsService(
    private val authenticationUserRepository: AuthenticationUserRepository,
) : ReactiveUserDetailsService {
    /**
     * @param userNameAndSource
     * @return IdentitySourceAwareUserDetails retrieved from UserDetails
     */
    override fun findByUsername(userNameAndSource: String): Mono<UserDetails> {
        val (name, source) = userNameAndSource.split(AUTH_SEPARATOR)
        return blockingToMono {
            authenticationUserRepository.findByNameAndSource(name, source)
        }
            .getIdentitySourceAwareUserDetails(name)
    }
}
