package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.utils.IdentitySourceAwareUserDetails
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
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
        .map<UserDetails> { user ->
            IdentitySourceAwareUserDetails(
                username = user.name!!,
                password = user.password ?: "",
                authorities = user.role
                    ?.split(',')
                    ?.filter { it.isNotBlank() }
                    ?.map { SimpleGrantedAuthority(it) }
                    ?: emptyList(),
                identitySource = user.source,
            )
        }
        .switchIfEmpty {
            Mono.error(UsernameNotFoundException(username))
        }
}
