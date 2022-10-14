package com.saveourtool.save.sandbox.service

import com.saveourtool.save.sandbox.repository.SandboxUserRepository
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
class SandboxUserDetailsService(
    private val sandboxUserRepository: SandboxUserRepository,
) : ReactiveUserDetailsService {
    /**
     * @param username
     * @return IdentitySourceAwareUserDetails retrieved from UserDetails
     */
    override fun findByUsername(username: String): Mono<UserDetails> = {
        sandboxUserRepository.findByName(username)
    }.toMono().getIdentitySourceAwareUserDetails(username)
}
