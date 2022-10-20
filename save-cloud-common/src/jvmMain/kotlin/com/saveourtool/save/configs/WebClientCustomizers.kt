package com.saveourtool.save.configs

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.readText

@Component
class WebClientCustomizers {
    @Bean
    @ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
    fun serviceAccountTokenHeaderWebClientCustomizer() = WebClientCustomizer { builder ->
        builder.filter { request, next ->
            val token = Path.of("/var/run/secrets/tokens/service-account-projected-token").readText()
            ClientRequest.from(request)
                .header("X-Service-Account-Token", token)
                .build()
                .let(next::exchange)
        }
    }
}