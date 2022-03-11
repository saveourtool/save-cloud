@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION",
)

package org.cqfn.save.orchestrator.config

import org.cqfn.save.utils.LocalDateTimeSerializer

import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.web.reactive.config.WebFluxConfigurer

import java.time.LocalDateTime

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

internal val json = Json {
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

@Configuration
class LocalDateTimeConfig {
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer() = Jackson2ObjectMapperBuilderCustomizer { jacksonObjectMapperBuilder: Jackson2ObjectMapperBuilder ->
        jacksonObjectMapperBuilder
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Bean
    fun kotlinSerializationJsonEncoder() = KotlinSerializationJsonEncoder(json)

    @Bean
    fun kotlinSerializationJsonDecoder() = KotlinSerializationJsonDecoder(json)

    @Bean
    fun webFluxConfigurer(encoder: KotlinSerializationJsonEncoder, decoder: KotlinSerializationJsonDecoder) =
            object : WebFluxConfigurer {
                override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
                    configurer.defaultCodecs().kotlinSerializationJsonEncoder(encoder)
                    configurer.defaultCodecs().kotlinSerializationJsonDecoder(decoder)
                }
            }
}
