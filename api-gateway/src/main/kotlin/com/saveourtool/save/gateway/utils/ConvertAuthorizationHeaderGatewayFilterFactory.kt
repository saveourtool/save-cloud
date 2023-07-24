package com.saveourtool.save.gateway.utils

import com.saveourtool.save.gateway.service.BackendService
import com.saveourtool.save.utils.AUTHORIZATION_SOURCE
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.Base64

/**
 * Filter, that mutate existing exchange to Basic,
 * inserts username into Authorization header instead of existing value, not paying attention to the credentials,
 * since at this moment they are already checked by gateway.
 * Also insert source data (where the user identity is coming from) into X-Authorization-Source header
 */
@Component
class ConvertAuthorizationHeaderGatewayFilterFactory(
    private val backendService: BackendService,
) : AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter = GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
        exchange.getPrincipal<Principal>()
            .flatMap { principal ->
                when (principal) {
                    is OAuth2AuthenticationToken -> backendService.findByOriginalLogin(principal.authorizedClientRegistrationId, principal.name)
                        .map { it.name to principal.authorizedClientRegistrationId }
                    is UsernamePasswordAuthenticationToken -> {
                        // Note: current authentication type we support only for save-api, which already set
                        // user source into X-Authorization-Source header, however, in general case
                        // we need to provide it here too, somehow
                        Mono.just(principal.name to null)
                    }
                    else -> Mono.error(BadCredentialsException("Unsupported authentication type: ${principal::class}"))
                }
            }
            .map { (name, source) ->
                exchange.mutate().request { builder ->
                    builder.headers { headers: HttpHeaders ->
                        headers.set(HttpHeaders.AUTHORIZATION, "Basic ${
                            Base64.getEncoder().encodeToString("$name:".toByteArray())
                        }")
                        source?.let { headers.set(AUTHORIZATION_SOURCE, it) }
                    }
                }
                    .build()
            }
            .defaultIfEmpty(exchange)
            .flatMap { chain.filter(it) }
    }
}
