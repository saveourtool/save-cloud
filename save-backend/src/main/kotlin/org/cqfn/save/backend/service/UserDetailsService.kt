package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.UserRepository
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * A service that provides `UserDetails`
 */
@Service
class UserDetailsService(
    private val userRepository: UserRepository,
) : ReactiveUserDetailsService {
    override fun findByUsername(username: String): Mono<UserDetails> = Mono.fromCallable {
        userRepository.findByName(username)
    }
        .filter { it.isPresent }
        .map { it.get() }
        .map {
            User.builder()
                .username(it.name)
                .password(it.password)
                .roles(it.role)
                .authorities(emptyList())
                .build()
        }
        .switchIfEmpty {
            Mono.error(UsernameNotFoundException(username))
        }
}
