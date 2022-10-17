package com.saveourtool.save.authservice.service

import com.saveourtool.save.authservice.repository.UserRepository
import com.saveourtool.save.utils.getIdentitySourceAwareUserDetails
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * A service that provides `UserDetails`
 */
@Service
class UserDetailsService(
    private val userRepository: UserRepository,
) : ReactiveUserDetailsService {
    /**
     * @param username
     * @return IdentitySourceAwareUserDetails retrieved from UserDetails
     */
    override fun findByUsername(username: String): Mono<UserDetails> = {
        userRepository.findByName(username)
    }.toMono().getIdentitySourceAwareUserDetails(username)
}
