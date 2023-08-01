package com.saveourtool.save.gateway.utils

import com.saveourtool.save.authservice.utils.AuthenticationUserDetails
import com.saveourtool.save.gateway.service.BackendService
import com.saveourtool.save.utils.AUTHORIZATION_ID
import com.saveourtool.save.utils.AUTHORIZATION_NAME
import com.saveourtool.save.utils.AUTHORIZATION_ROLES
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
import reactor.kotlin.core.publisher.toMono
import java.security.Principal

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
                    is UsernamePasswordAuthenticationToken -> (principal.principal as? AuthenticationUserDetails).toMono()
                    else -> Mono.error(BadCredentialsException("Unsupported authentication type: ${principal::class}"))
                }
            }
            .map { user ->
                exchange.mutate().request { builder ->
                    builder.headers { headers: HttpHeaders ->
                        headers.set(AUTHORIZATION_ID, user.id.toString())
                        headers.set(AUTHORIZATION_NAME, user.name)
                        headers.set(AUTHORIZATION_ROLES, user.role)
                    }
                }
                    .build()
            }
            .defaultIfEmpty(exchange)
            .flatMap { chain.filter(it) }
    }
}
