package com.saveourtool.save.gateway.service

import com.saveourtool.save.authservice.utils.SaveUserDetails
import com.saveourtool.save.entities.User
import com.saveourtool.save.gateway.config.ConfigurationProperties
import com.saveourtool.save.utils.orNotFound

import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * A service to backend to lookup users in DB
 */
@Service
class BackendService(
    configurationProperties: ConfigurationProperties,
) {
    private val webClient = WebClient.create(configurationProperties.backend.url)

    /**
     * @param username
     * @return [UserDetails] found in DB by received name
     */
    fun findByName(
        username: String,
    ): Mono<SaveUserDetails> = findAuthenticationUserDetails("/internal/users/find-by-name/$username")

    /**
     * @param source
     * @param nameInSource
     * @return [UserDetails] found in DB by source and name in this source
     */
    fun findByOriginalLogin(
        source: String,
        nameInSource: String,
    ): Mono<SaveUserDetails> = findAuthenticationUserDetails("/internal/users/find-by-original-login/$source/$nameInSource")

    private fun findAuthenticationUserDetails(uri: String): Mono<SaveUserDetails> = webClient.get()
        .uri(uri)
        .retrieve()
        .onStatus({ it.is4xxClientError }) {
            Mono.error(ResponseStatusException(it.statusCode()))
        }
        .toEntity<SaveUserDetails>()
        .flatMap { responseEntity ->
            responseEntity.body.toMono().orNotFound { "Authentication body is empty" }
        }

    /**
     * Find current user [SaveUserDetails] by [authentication].
     *
     * @param authentication current user [Authentication]
     * @return current user [SaveUserDetails]
     */
    fun findByAuthentication(authentication: Authentication): Mono<SaveUserDetails> = when (authentication) {
        is UsernamePasswordAuthenticationToken -> findByName(authentication.name)
        is OAuth2AuthenticationToken -> {
            val source = authentication.authorizedClientRegistrationId
            val nameInSource = authentication.name
            findByOriginalLogin(source, nameInSource)
        }
        else -> Mono.empty()
    }

    /**
     * Find current username by [authentication].
     *
     * @param authentication current user [Authentication]
     * @return current username
     */
    fun findNameByAuthentication(authentication: Authentication?): Mono<String> = when (authentication) {
        is UsernamePasswordAuthenticationToken -> authentication.name.toMono()
        is OAuth2AuthenticationToken -> {
            val source = authentication.authorizedClientRegistrationId
            val nameInSource = authentication.name
            findByOriginalLogin(source, nameInSource).map { it.name }
        }
        else -> Mono.empty()
    }

    /**
     * Saves a new [User] in DB
     *
     * @param source
     * @param nameInSource
     * @return empty [Mono]
     */
    fun createNewIfRequired(source: String, nameInSource: String): Mono<Void> = webClient.post()
        .uri("/internal/users/new/$source/$nameInSource")
        .contentType(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus({ it.is4xxClientError }) {
            Mono.error(ResponseStatusException(it.statusCode()))
        }
        .toBodilessEntity()
        .then()
}
