package com.saveourtool.save.authservice.utils

import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.getLogger

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val logger = getLogger<IdentitySourceAwareUserDetails>()

/**
 * @param authorities comma-separated authorities as a single string
 * @property identitySource where the user identity is coming from
 * @property id
 */
class IdentitySourceAwareUserDetails(
    username: String,
    password: String?,
    authorities: String?,
    val identitySource: String,
    val id: Long,
) : SpringUser(
    username,
    password,
    authorities?.split(',')
        ?.filter { it.isNotBlank() }
        ?.map { SimpleGrantedAuthority(it) }
        .orEmpty()
)

/**
 * @param usernameSupplier
 * @return mono of IdentitySourceAwareUserDetails, retrieved from save-cloud User entity
 */
fun Mono<User>.mapToIdentitySourceAwareUserDetailsOrNotFound(usernameSupplier: () -> String) = this
    .map<UserDetails> { user ->
        user.toIdentitySourceAwareUserDetails()
    }
    .switchIfEmpty {
        usernameSupplier.toMono()
            .flatMap { username ->
                logger.warn("Couldn't find user with name=$username in DB!")
                Mono.error(UsernameNotFoundException(username))
            }
    }

/**
 * @return IdentitySourceAwareUserDetails, retrieved from save-cloud User entity
 */
@Suppress("UnsafeCallOnNullableType")
private fun User.toIdentitySourceAwareUserDetails(): IdentitySourceAwareUserDetails = IdentitySourceAwareUserDetails(
    username = this.name,
    password = this.password.orEmpty(),
    authorities = this.role,
    identitySource = this.source,
    id = this.id!!,
)
