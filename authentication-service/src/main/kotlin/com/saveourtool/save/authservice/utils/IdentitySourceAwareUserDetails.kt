package com.saveourtool.save.authservice.utils

import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.getLogger

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

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
 * @param username
 * @param source where the user identity is coming from
 * @return mono of IdentitySourceAwareUserDetails, retrieved from save-cloud User entity
 */
fun Mono<User>.getIdentitySourceAwareUserDetails(username: String, source: String? = null) = this
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

/**
 * @return IdentitySourceAwareUserDetails, retrieved from save-cloud User entity
 */
@Suppress("UnsafeCallOnNullableType")
private fun User.toIdentitySourceAwareUserDetails(): IdentitySourceAwareUserDetails = IdentitySourceAwareUserDetails(
    username = this.name!!,
    password = this.password.orEmpty(),
    authorities = this.role,
    identitySource = this.source,
    id = this.id!!,
)
