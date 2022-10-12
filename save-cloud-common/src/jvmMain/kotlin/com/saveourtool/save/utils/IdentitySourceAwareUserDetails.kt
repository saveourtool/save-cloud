package com.saveourtool.save.utils

import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

private val logger = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @param authorities comma-separated authorities as a single string
 * @property identitySource
 * @property id
 */
class IdentitySourceAwareUserDetails(
    username: String,
    password: String?,
    authorities: String?,
    val identitySource: String,
    val id: Long,
) : org.springframework.security.core.userdetails.User(
    username,
    password,
    authorities?.split(',')
        ?.filter { it.isNotBlank() }
        ?.map { SimpleGrantedAuthority(it) }
        .orEmpty()
)

/**
 * @return IdentitySourceAwareUserDetails, retrieved from save-cloud User entity
 */
@Suppress("UnsafeCallOnNullableType")
fun com.saveourtool.save.entities.User.toIdentitySourceAwareUserDetails(): IdentitySourceAwareUserDetails = IdentitySourceAwareUserDetails(
    username = this.name!!,
    password = this.password.orEmpty(),
    authorities = this.role,
    identitySource = this.source,
    id = this.id!!,
)

/**
 * @param username
 * @param source
 * @return mono of IdentitySourceAwareUserDetails, retrieved from save-cloud User entity
 */
fun Mono<com.saveourtool.save.entities.User>.getIdentitySourceAwareUserDetails(username: String, source: String? = null) = this
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
