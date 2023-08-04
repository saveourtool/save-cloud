package com.saveourtool.save.gateway.utils

import com.saveourtool.save.authservice.utils.AuthenticationUserDetails
import com.saveourtool.save.gateway.service.BackendService
import com.saveourtool.save.utils.switchIfEmptyToResponseException
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.security.Principal

/**
 * Filter, that mutate existing exchange,
 * inserts user's info into Authorization headers instead of existing value, not paying attention to the credentials,
 * since at this moment they are already checked by gateway.
 */
@Component
class AuthorizationHeadersGatewayFilterFactory(
    private val backendService: BackendService,
) : AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter = GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
        exchange.getPrincipal<Principal>()
            .flatMap { resolveSaveUser(it) }
            .map { user ->
                exchange.mutate()
                    .request { builder ->
                        builder.headers { headers: HttpHeaders ->
                            headers.remove(HttpHeaders.AUTHORIZATION)
                            user.populateHeaders(headers)
                        }
                    }
                    .build()
            }
            .defaultIfEmpty(exchange)
            .flatMap { chain.filter(it) }
    }

    private fun resolveSaveUser(principal: Principal): Mono<AuthenticationUserDetails> = when (principal) {
        is OAuth2AuthenticationToken -> backendService.findByOriginalLogin(principal.authorizedClientRegistrationId, principal.name)
        is UsernamePasswordAuthenticationToken -> (principal.principal as? AuthenticationUserDetails)
            .toMono()
            .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR) {
                "Unexpected principal type ${principal.principal.javaClass} in ${UsernamePasswordAuthenticationToken::class}"
            }
        else -> Mono.error(BadCredentialsException("Unsupported authentication type: ${principal::class}"))
    }
}
