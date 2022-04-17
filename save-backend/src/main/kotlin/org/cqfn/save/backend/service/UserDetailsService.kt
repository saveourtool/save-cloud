package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.utils.IdentitySourceAwareUserDetails
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
    @Suppress("UnsafeCallOnNullableType")
    override fun findByUsername(username: String): Mono<UserDetails> = Mono.fromCallable {
        userRepository.findByName(username)
    }
        .filter { it.isPresent }
        .map { it.get() }
        .map<UserDetails> { user ->
            IdentitySourceAwareUserDetails(
                username = user.name!!,
                password = user.password ?: "",
                authorities = user.role,
                identitySource = user.source,
                id = user.id!!,
            )
        }
        .switchIfEmpty {
            Mono.error(UsernameNotFoundException(username))
        }

    /**
     * @param name
     * @param relativePath
     * @throws NoSuchElementException
     */
    fun saveAvatar(name: String, relativePath: String) {
        val user = userRepository.findByName(name).get().apply {
            avatar = relativePath
        }
        user.let { userRepository.save(it) }
    }
}
