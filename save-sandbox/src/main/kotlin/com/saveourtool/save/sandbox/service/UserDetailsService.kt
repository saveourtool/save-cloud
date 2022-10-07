package com.saveourtool.save.sandbox.service


import com.saveourtool.save.entities.User
import com.saveourtool.save.sandbox.repository.SandboxUserRepository
import com.saveourtool.save.utils.IdentitySourceAwareUserDetails
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

/**
 * A service that provides `UserDetails`
 */
@Service
class SandboxUserDetailsService(
    private val sandboxUserRepository: SandboxUserRepository,
) : ReactiveUserDetailsService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun findByUsername(username: String): Mono<UserDetails> = {
        sandboxUserRepository.findByName(username)
    }.toMono().getIdentitySourceAwareUserDetails(username)

    /**
     * @param username
     * @param source source (where the user identity is coming from)
     * @return IdentitySourceAwareUserDetails retrieved from UserDetails
     */
    fun findByUsernameAndSource(username: String, source: String) =
        { sandboxUserRepository.findByNameAndSource(username, source) }
            .toMono()
            .getIdentitySourceAwareUserDetails(username, source)


    // TODO: MOVE TO COMMON
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

    // TODO: MOVE TO COMMON
    @Suppress("UnsafeCallOnNullableType")
    private fun User.toIdentitySourceAwareUserDetails(): IdentitySourceAwareUserDetails = IdentitySourceAwareUserDetails(
        username = this.name!!,
        password = this.password.orEmpty(),
        authorities = this.role,
        identitySource = this.source,
        id = this.id!!,
    )
}
