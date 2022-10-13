package com.saveourtool.save.backend.configs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.security.Principal

/**
 * A web filter that logs details of user that performed a request, if any.
 */
@Component
class UserLoggingFilter : WebFilter {
    private val logger = LoggerFactory.getLogger(UserLoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> = exchange.getPrincipal<Principal>().doOnNext {
        logger.debug("${exchange.request.method} ${exchange.request.path} request authorized by $it")
    }.then(chain.filter(exchange))
}
