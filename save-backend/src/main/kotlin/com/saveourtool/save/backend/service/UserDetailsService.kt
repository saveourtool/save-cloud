package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.UserSaveStatus
import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.IdentitySourceAwareUserDetails
import com.saveourtool.save.utils.orNotFound
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.util.*

/**
 * A service that provides `UserDetails`
 */
@Service
class UserDetailsService(
    private val userRepository: UserRepository,
    private val originalLoginRepository: OriginalLoginRepository,
) : ReactiveUserDetailsService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun findByUsername(username: String): Mono<UserDetails> =
            userRepository.findByName(username)?.let {
                it.toMono()
                    .getIdentitySourceAwareUserDetails(username)
            } ?: run {
                originalLoginRepository.findByName(username)
                    .toMono()
                    .map { it.user }
                    .getIdentitySourceAwareUserDetails(username)
            }

    /**
     * @param username
     * @param source source (where the user identity is coming from)
     * @return IdentitySourceAwareUserDetails retrieved from UserDetails
     */
    fun findByUsernameAndSource(username: String, source: String) =
            { originalLoginRepository.findByNameAndSource(username, source) }
                .toMono()
                .map { it.user }
                .getIdentitySourceAwareUserDetails(username, source)

    /**
     * @param name
     * @param relativePath
     * @throws NoSuchElementException
     */
    fun saveAvatar(name: String, relativePath: String) {
        val user = userRepository.findByName(name)
            .orNotFound()
            .apply {
                avatar = relativePath
            }
        user.let { userRepository.save(it) }
    }

    private fun Mono<User>.getIdentitySourceAwareUserDetails(username: String, source: String? = null) = this
        .map<UserDetails> { user ->
            user.toIdentitySourceAwareUserDetails()
        }
        .switchIfEmpty {
            Mono.fromCallable {
                val sourceMsg = source?.let {
                    " and source=$source"
                }.orEmpty()
                logger.warn("Couldn't find user with name=$username$sourceMsg in DB!")
            }.flatMap {
                Mono.error(UsernameNotFoundException(username))
            }
        }
    @Suppress("UnsafeCallOnNullableType")
    private fun User.toIdentitySourceAwareUserDetails(): IdentitySourceAwareUserDetails = IdentitySourceAwareUserDetails(
        username = this.name!!,
        password = this.password.orEmpty(),
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

    fun saveUser(user: User): UserSaveStatus {
        val userName = user.name
        return  if (userName != null && userRepository.validateName(userName) != 0L) {
            userRepository.saveHighName(userName)
            userRepository.save(user)
            UserSaveStatus.UPDATE
        } else {
            UserSaveStatus.CONFLICT
        }
    }

}
