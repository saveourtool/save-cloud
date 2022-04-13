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
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = apply(exchange).map { authentication ->
        val name = (authentication as UsernamePasswordAuthenticationToken).principal as String
        val source = exchange.request.headers["X-Authorization-Source"]?.firstOrNull()
        UsernamePasswordAuthenticationToken(
            "$source:$name",
            authentication.credentials as String
        ).apply {
            details = AuthenticationDetails(
                id = -1,
                identitySource = source,
            )
        }
    }
}
