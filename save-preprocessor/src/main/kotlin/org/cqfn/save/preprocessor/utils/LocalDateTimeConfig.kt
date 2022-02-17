@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "KDOC_NO_EMPTY_TAGS",
    "KDOC_WITHOUT_PARAM_TAG"
)

package org.cqfn.save.preprocessor.utils

import org.cqfn.save.utils.LocalDateTimeSerializer

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.web.reactive.config.WebFluxConfigurer

import java.time.LocalDateTime

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

internal val json = Json {
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

@Configuration
class LocalDateTimeConfig {

    @Bean
    fun kotlinSerializationJsonEncoder() = KotlinSerializationJsonEncoder()

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
