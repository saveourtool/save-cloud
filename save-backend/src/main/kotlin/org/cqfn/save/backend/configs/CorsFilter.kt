package org.cqfn.save.backend.configs

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class CorsFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (exchange != null) {
            exchange.response.headers.add("Access-Control-Allow-Origin", "*")
            exchange.response.headers.add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS")
            exchange.response.headers.add("Access-Control-Allow-Headers", "DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Content-Range,Range")
            if (exchange.request.method == HttpMethod.OPTIONS) {
                exchange.response.headers.add("Access-Control-Max-Age", "1728000")
                exchange.response.statusCode = HttpStatus.NO_CONTENT
                return Mono.empty()
            } else {
                exchange.response.headers.add("Access-Control-Expose-Headers", "DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Content-Range,Range")
                return chain?.filter(exchange) ?: Mono.empty()
            }
        } else {
            return chain?.filter(exchange) ?: Mono.empty()
        }
    }
}