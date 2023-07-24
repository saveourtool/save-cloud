package com.saveourtool.save.gateway.service

import com.saveourtool.save.entities.User
import com.saveourtool.save.gateway.config.ConfigurationProperties

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * A service to backend to lookup users in DB
 */
@Service
class BackendService(
    configurationProperties: ConfigurationProperties,
    objectMapper: ObjectMapper,
) {
    private val springUserDetailsReader = objectMapper
        .also {
            it.registerModules(SecurityJackson2Modules.getModules(javaClass.classLoader))
        }
        .readerFor(SpringUser::class.java)
    private val webClient = WebClient.create(configurationProperties.backend.url)

    /**
     * @param username
     * @return [UserDetails] found in DB by received name
     */
    fun findByName(username: String): Mono<UserDetails> = webClient.get()
        .uri("/internal/users/find-by-name/$username")
        .retrieve()
        .onStatus({ it.is4xxClientError }) {
            Mono.error(ResponseStatusException(it.statusCode()))
        }
        .toEntity<String>()
        .map {
            springUserDetailsReader.readValue(it.body)
        }

    /**
     * @param source
     * @param nameInSource
     * @return [UserDetails] found in DB by source and name in this source
     */
    fun findByOriginalLogin(source: String, nameInSource: String): Mono<UserDetails> = webClient.get()
        .uri("/internal/users/find-by-original-login/$source/$nameInSource")
        .retrieve()
        .onStatus({ it.is4xxClientError }) {
            Mono.error(ResponseStatusException(it.statusCode()))
        }
        .toEntity<String>()
        .map {
            springUserDetailsReader.readValue(it.body)
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
