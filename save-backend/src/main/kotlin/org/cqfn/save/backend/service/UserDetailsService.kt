package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.entities.User
import org.cqfn.save.utils.IdentitySourceAwareUserDetails
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.*

/**
 * A service that provides `UserDetails`
 */
@Service
class UserDetailsService(
    private val userRepository: UserRepository,
) : ReactiveUserDetailsService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Suppress("UnsafeCallOnNullableType")
    override fun findByUsername(username: String): Mono<UserDetails> = Mono.fromCallable {
        userRepository.findByName(username)
    }.getIdentitySourceAwareUserDetails(username)

    fun findByUsernameAndSource(username: String, source: String) = Mono.fromCallable {
        userRepository.findByNameAndSource(username, source)
    }.getIdentitySourceAwareUserDetails(username, source)

    private fun Mono<Optional<User>>.getIdentitySourceAwareUserDetails(username: String, source: String? = null) = this.filter {
        if (!it.isPresent) {
            val sourceMsg = source?.let {
                " and source=$source "
            } ?: ""
            logger.warn("Couldn't find user with name=${username}$sourceMsg in DB!")
        }
        it.isPresent
    }
        .map { it.get() }
        .map<UserDetails> { user ->
            user.toIdentitySourceAwareUserDetails(source)
        }
        .switchIfEmpty {
            Mono.error(UsernameNotFoundException(username))
        }

    private fun User.toIdentitySourceAwareUserDetails(source: String?): IdentitySourceAwareUserDetails {
        println("\n\nGetting the user ${this.name} ${this.password} ${this.role} ${this.source}")
        return IdentitySourceAwareUserDetails(
            username = this.name!!,
            password = this.password ?: "",
            authorities = this.role,
            identitySource = this.source,
            id = this.id!!,
        )
    }
}
