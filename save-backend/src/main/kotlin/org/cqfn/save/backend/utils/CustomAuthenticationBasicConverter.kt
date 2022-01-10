package org.cqfn.save.backend.utils

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Implementation of [ServerAuthenticationConverter] that embeds user identity source into [UsernamePasswordAuthenticationToken]
 */
@Component
class CustomAuthenticationBasicConverter : org.springframework.security.web.server.ServerHttpBasicAuthenticationConverter(),
ServerAuthenticationConverter {
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = apply(exchange).map {
        val name = (it as UsernamePasswordAuthenticationToken).principal as String
        val source = exchange.request.headers["X-Authorization-Source"]?.firstOrNull()
        UsernamePasswordAuthenticationToken(
            "$source:$name",
            it.credentials as String
        ).apply {
            details = mapOf(
                "identitySource" to source
            )
        }
    }
}
