package org.cqfn.save.gateway.utils

import org.cqfn.save.domain.Role
import org.cqfn.save.entities.User
import org.cqfn.save.gateway.config.ConfigurationProperties

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module
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
    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerModule(CoreJackson2Module())
        .registerModule(OAuth2ClientJackson2Module())
    private val webClient = WebClient.create(configurationProperties.backend.url)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        logger.info("Authenticated user ${authentication.userName()} with authentication type ${authentication::class}, will send data to backend")

        val user = authentication.toUser().apply {
            // https://github.com/analysis-dev/save-cloud/issues/583
            // fixme: this sets a default role for a new user with minimal scope, however this way we discard existing role
            // from authentication provider. In the future we may want to use this information and have a mapping of existing
            // roles to save-cloud roles.
            role = Role.VIEWER.asSpringSecurityRole()
        }
        return webClient.post()
            .uri("/internal/users/new")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(user))
            .retrieve()
            .onStatus({ it.is4xxClientError }) {
                Mono.error(ResponseStatusException(it.statusCode()))
            }
            .toBodilessEntity()
            .then()
    }
}

/**
 * @return [User] with data from this [Authentication]
 */
fun Authentication.toUser(): User = User(
    userName(),
    null,
    authorities.joinToString(",") { it.authority },
    toIdentitySource(),
)
