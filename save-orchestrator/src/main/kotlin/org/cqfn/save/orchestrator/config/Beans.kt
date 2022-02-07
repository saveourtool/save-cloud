package org.cqfn.save.orchestrator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration class with various beans
 */
@Configuration
class Beans(private val configProperties: ConfigProperties) {
    /**
     * Used to send requests to backend
     *
     * @return [WebClient] with backend URL
     */
    @Bean
    fun webClientBackend() = WebClient.create(configProperties.backendUrl)
//    fun webClientBackend() = WebClient.builder()
//        .exchangeStrategies(
//            ExchangeStrategies.builder()
//            .codecs { configurer: ClientCodecConfigurer ->
//                configurer
//                    .defaultCodecs()
//                    .maxInMemorySize(100 * 1024 * 1024)
//            }
//                .build()
//        )
//        .baseUrl(configProperties.backendUrl)
//        .build()

}
