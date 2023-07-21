package com.saveourtool.save.gateway.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.gateway.service.BackendService

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import reactor.core.publisher.Mono

/**
 * [ServerAuthenticationSuccessHandler] that sends user data to backend on successful login
 */
class StoringServerAuthenticationSuccessHandler(
    private val backendService: BackendService,
) : ServerAuthenticationSuccessHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        logger.info("Authenticated user ${authentication.name} with authentication type ${authentication::class}, will send data to backend")

        val (source, nameInSource) = if (authentication is OAuth2AuthenticationToken) {
            authentication.authorizedClientRegistrationId to authentication.principal.name
        } else {
            throw BadCredentialsException("Not supported authentication type ${authentication::class}")
        }
        // https://github.com/saveourtool/save-cloud/issues/583
        // fixme: this sets a default role for a new user with minimal scope, however this way we discard existing role
        // from authentication provider. In the future we may want to use this information and have a mapping of existing
        // roles to save-cloud roles (authentication.authorities.map { it.authority }).
        val roles = listOf(Role.VIEWER.asSpringSecurityRole())

        return backendService.createNewIfRequired(user)
    }
}
