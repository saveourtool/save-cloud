package org.cqfn.save.gateway.utils

import org.cqfn.save.utils.extractUserNameAndSource
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.Base64

/**
 * Filter, that inserts username into Authorization header instead of existing value.
 */
@Component
class ConvertAuthorizationHeaderGatewayFilterFactory : AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter = GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
        println("\n====================ConvertAuthorizationHeaderGatewayFilterFactory apply")
        exchange.getPrincipal<Principal>().map { principal ->
                val credentials = when (principal) {
                    is OAuth2AuthenticationToken -> {
                        principal.userName() to (principal as? OAuth2AuthenticationToken)?.authorizedClientRegistrationId
                    }
                    is UsernamePasswordAuthenticationToken -> {
                        //val (name, source) = extractUserNameAndSource(principal.userName())
                        //name to null//source
                        principal.userName() to null//source
                    }
                    else -> {
                        //TODO: any exception?
                        principal.userName() to null
                    }
                }
                credentials
            }
            .map { (name, source) ->
                exchange.mutate().request { request ->
                    request.headers { headers: HttpHeaders ->
                        println("\n\n\nSET HEADERS $name $source ${headers.get("X-Authorization-Source")}")
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
