package com.saveourtool.save.gateway.service

import com.saveourtool.save.authservice.utils.AuthenticationUserDetails
import com.saveourtool.save.entities.User
import com.saveourtool.save.gateway.config.ConfigurationProperties
import com.saveourtool.save.utils.orNotFound

import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetails
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
    ): Mono<AuthenticationUserDetails> = findAuthenticationUserDetails("/internal/users/find-by-name/$username")

    /**
     * @param source
     * @param nameInSource
     * @return [UserDetails] found in DB by source and name in this source
     */
    fun findByOriginalLogin(
        source: String,
        nameInSource: String,
    ): Mono<AuthenticationUserDetails> = findAuthenticationUserDetails("/internal/users/find-by-original-login/$source/$nameInSource")

    private fun findAuthenticationUserDetails(uri: String): Mono<AuthenticationUserDetails> = webClient.get()
        .uri(uri)
        .retrieve()
        .onStatus({ it.is4xxClientError }) {
            Mono.error(ResponseStatusException(it.statusCode()))
        }
        .toEntity<AuthenticationUserDetails>()
        .flatMap { responseEntity ->
            responseEntity.body.toMono().orNotFound { "Body is empty" }
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
