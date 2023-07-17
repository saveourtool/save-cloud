package com.saveourtool.save.gateway.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.gateway.config.ConfigurationProperties

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * [ServerAuthenticationSuccessHandler] that sends user data to backend on successful login
 */
class StoringServerAuthenticationSuccessHandler(
    configurationProperties: ConfigurationProperties,
) : ServerAuthenticationSuccessHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.create(configurationProperties.backend.url)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        logger.info("Authenticated user ${authentication.userName()} with authentication type ${authentication::class}, will send data to backend")

        val source = authentication.toIdentitySource()
        val nameInSource = authentication.userName()
        // https://github.com/saveourtool/save-cloud/issues/583
        // fixme: this sets a default role for a new user with minimal scope, however this way we discard existing role
        // from authentication provider. In the future we may want to use this information and have a mapping of existing
        // roles to save-cloud roles (authentication.authorities.map { it.authority }).
        val roles = listOf(Role.VIEWER.asSpringSecurityRole())

        return webClient.post()
            .uri("/internal/users/${source}/${nameInSource}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(roles)
            .retrieve()
            .onStatus({ it.is4xxClientError }) {
                Mono.error(ResponseStatusException(it.statusCode()))
            }
            .toBodilessEntity()
            .then()
    }
}
