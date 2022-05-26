package com.saveourtool.save.gateway.utils

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.security.Principal
import java.util.Base64

/**
 * Filter, that mutate existing exchange to Basic,
 * inserts username into Authorization header instead of existing value, not paying attention to the credentials,
 * since at this moment they are already checked by gateway.
 * Also insert source data (where the user identity is coming from) into X-Authorization-Source header
 */
@Component
class ConvertAuthorizationHeaderGatewayFilterFactory : AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter = GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
        exchange.getPrincipal<Principal>().map { principal ->
            val credentials = when (principal) {
                is OAuth2AuthenticationToken -> principal.userName() to (principal as? OAuth2AuthenticationToken)?.authorizedClientRegistrationId
                is UsernamePasswordAuthenticationToken -> {
                    // Note: current authentication type we support only for save-api, which already set
                    // user source into X-Authorization-Source header, however, in general case
                    // we need to provide it here too, somehow
                    principal.userName() to null
                }
                else -> throw BadCredentialsException("Unsupported authentication type: ${principal::class}")
            }
            credentials
        }
            .map { (name, source) ->
                exchange.mutate().request { builder ->
                    builder.headers { headers: HttpHeaders ->
                        headers.set(HttpHeaders.AUTHORIZATION, "Basic ${
                            Base64.getEncoder().encodeToString("$name:".toByteArray())
                        }")
                        source?.let { headers.set("X-Authorization-Source", it) }
                    }
                }
                    .build()
            }
            .defaultIfEmpty(exchange)
            .flatMap { chain.filter(it) }
    }
}
