package com.saveourtool.save.authservice.security

import com.saveourtool.save.authservice.service.AuthenticationUserDetailsService
import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.authservice.utils.IdentitySourceAwareUserDetails
import com.saveourtool.save.utils.AUTHORIZATION_SOURCE
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerHttpBasicAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast

/**
 * Implementation of [ServerAuthenticationConverter] that embeds user identity source into [UsernamePasswordAuthenticationToken]
 */
@Component
class CustomAuthenticationBasicConverter(
    private val authenticationUserDetailsService: AuthenticationUserDetailsService
) : ServerHttpBasicAuthenticationConverter(), ServerAuthenticationConverter {
    /**
     * Convert exchange, received from gateway into UsernamePasswordAuthenticationToken, specify source identity, laid
     * by gateway into X-Authorization-Source header
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = super.convert(exchange)
        .cast<UsernamePasswordAuthenticationToken>()
        .flatMap { authentication ->
            val username = authentication.principal as String
            val source = exchange.request.headers[AUTHORIZATION_SOURCE]?.firstOrNull()
            authenticationUserDetailsService.findByUsername(username)
                .cast<IdentitySourceAwareUserDetails>()
                .map { userDetails ->
                    UsernamePasswordAuthenticationToken(
                        "$source:$username",
                        authentication.credentials,
                        userDetails.authorities
                    ).apply {
                        details = AuthenticationDetails(
                            id = userDetails.id,
                            identitySource = source,
                        )
                    }
                }
        }
}
