package com.saveourtool.save.gateway.utils

import com.saveourtool.save.gateway.service.BackendService
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
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
            .flatMap { principal ->
                exchange.session.flatMap { session ->
                    backendService.findByPrincipal(principal, session)
                }
            }
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
}
