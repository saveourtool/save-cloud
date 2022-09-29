package com.saveourtool.save.preprocessor.filters

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.nio.file.Path
import kotlin.io.path.readText

@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
@Component
class ServiceTokenHeaderResponseFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = Path.of("/var/run/secrets/tokens/service-account-projected-token").readText()
        exchange.response.headers
            .add("X-Service-Account-Token", token)
        return chain.filter(exchange)
    }
}
