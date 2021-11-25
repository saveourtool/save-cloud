package org.cqfn.save.gateway.security

import org.cqfn.save.gateway.config.ConfigurationProperties

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.jackson2.CoreJackson2Module
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * A service that provides `UserDetails`
 */
@Service
class RemoteUserDetailsService(
    configurationProperties: ConfigurationProperties,
) : ReactiveUserDetailsService {
    private val objectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerModule(CoreJackson2Module())
    private val webClient: WebClient = WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder().codecs {
                it.defaultCodecs()
                    .jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
            }
                .build()
        )
        .baseUrl(configurationProperties.backend.url)
        .build()

    override fun findByUsername(username: String): Mono<UserDetails> =
            webClient.get()
                .uri("/user/details?username=$username")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono<User>()
                .map { it as UserDetails }
                .log()
}
