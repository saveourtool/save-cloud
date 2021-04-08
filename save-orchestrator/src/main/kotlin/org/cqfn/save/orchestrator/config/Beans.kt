package org.cqfn.save.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class Beans(private val configProperties: ConfigProperties) {
    /**
     * Used to send requests to backend
     */
    @Bean
    fun webClientBackend() = WebClient.create(configProperties.backendUrl)
}
