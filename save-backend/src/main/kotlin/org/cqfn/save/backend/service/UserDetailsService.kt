package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.User
import org.cqfn.save.utils.IdentitySourceAwareUserDetails
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
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

    override fun findByUsername(username: String): Mono<UserDetails> = Mono.fromCallable {
        userRepository.findByName(username)
    }.getIdentitySourceAwareUserDetails(username)

    /**
     * @param username
     * @param source source (where the user identity is coming from)
     * @return IdentitySourceAwareUserDetails retrieved from UserDetails
     */
    fun findByUsernameAndSource(username: String, source: String) = Mono.fromCallable {
        userRepository.findByNameAndSource(username, source)
    }.getIdentitySourceAwareUserDetails(username, source)

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

    private fun Mono<Optional<User>>.getIdentitySourceAwareUserDetails(username: String, source: String? = null) = this.filter { user ->
        if (!user.isPresent) {
            val sourceMsg = source?.let {
                " and source=$source"
            } ?: ""
            logger.warn("Couldn't find user with name=$username$sourceMsg in DB!")
        }
        user.isPresent
    }
        .map { it.get() }
        .map<UserDetails> { user ->
            user.toIdentitySourceAwareUserDetails()
        }
        .switchIfEmpty {
            Mono.error(UsernameNotFoundException(username))
        }
    @Suppress("UnsafeCallOnNullableType")
    private fun User.toIdentitySourceAwareUserDetails(): IdentitySourceAwareUserDetails = IdentitySourceAwareUserDetails(
        username = this.name!!,
        password = this.password ?: "",
        authorities = this.role,
        identitySource = this.source,
        id = this.id!!,
    )

    /**
     * @param authentication
     * @return global [Role] of authenticated user
     */
    fun getGlobalRole(authentication: Authentication): Role = authentication.authorities
        .map { grantedAuthority ->
            Role.values().find { role -> role.asSpringSecurityRole() == grantedAuthority.authority }
        }
        .sortedBy { it?.priority }
        .lastOrNull()
        ?: Role.VIEWER
}
