package com.saveourtool.save.authservice.security

import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.utils.AUTHORIZATION_SOURCE
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerHttpBasicAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Implementation of [ServerAuthenticationConverter] that embeds user identity source into [UsernamePasswordAuthenticationToken]
 */
@Component
class CustomAuthenticationBasicConverter : ServerHttpBasicAuthenticationConverter(),
    ServerAuthenticationConverter {
    /**
     * Convert exchange, received from gateway into UsernamePasswordAuthenticationToken, specify source identity, laid
     * by gateway into X-Authorization-Source header
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = super.convert(exchange).map { authentication ->
        val name = (authentication as UsernamePasswordAuthenticationToken).principal as String
        UsernamePasswordAuthenticationToken(
            name,
            authentication.credentials as String
        ).apply {
            details = AuthenticationDetails(
                id = -1L,
            )
        }
    }
}