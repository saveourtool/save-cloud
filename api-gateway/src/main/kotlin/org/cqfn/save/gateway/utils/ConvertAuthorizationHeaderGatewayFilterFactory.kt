package org.cqfn.save.gateway.utils

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.security.Principal
import java.util.Base64

/**
 * Filter, that inserts username into Authorization header instead of existing value.
 */
@Component
class ConvertAuthorizationHeaderGatewayFilterFactory : AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter = GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
        exchange.getPrincipal<Principal>()
            .map { it.userName() to (it as? OAuth2AuthenticationToken)?.authorizedClientRegistrationId }
            .map { (name, source) ->
                exchange.mutate().request {
                    it.headers { headers: HttpHeaders ->
                        println("\n\n\nSET HEADERS ${name}")
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
