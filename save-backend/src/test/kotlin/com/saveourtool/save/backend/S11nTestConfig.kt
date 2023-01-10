package com.saveourtool.save.backend

import org.reactivestreams.Publisher
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.core.ResolvableType
import org.springframework.core.codec.AbstractDecoder
import org.springframework.core.codec.AbstractEncoder
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.codec.CodecConfigurer.DefaultCodecs
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.util.MimeType
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactor.core.publisher.Flux

/**
 * Extra S11N (serialization) configuration used in tests (disables `kotlinx.serialization`
 * which may accidentally be present on the classpath while _Jackson_ is used).
 */
@TestConfiguration
class S11nTestConfig {
    /**
     * Disables `kotlinx.serialization` on the server side.
     */
    @Bean
    fun kotlinSerializationServerConfig(): WebFluxConfigurer =
            object : WebFluxConfigurer {
                override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
                    configurer.defaultCodecs().disableKotlinxSerialization()
                }
            }

    /**
     * Disables `kotlinx.serialization` on the client side.
     */
    @Bean
    fun kotlinSerializationClientConfig(): WebTestClientBuilderCustomizer =
            WebTestClientBuilderCustomizer { builder ->
                builder.codecs { configurer ->
                    configurer.defaultCodecs().disableKotlinxSerialization()
                }
            }

    /**
     * Replaces the encoder and the decoder with no-op values.
     */
    private fun DefaultCodecs.disableKotlinxSerialization() {
        kotlinSerializationJsonEncoder(noOpJsonEncoder())
        kotlinSerializationJsonDecoder(noOpJsonDecoder())
    }

    private fun noOpJsonDecoder(): AbstractDecoder<Any> =
            object : AbstractDecoder<Any>() {
                override fun canDecode(
                    elementType: ResolvableType,
                    mimeType: MimeType?,
                ): Boolean =
                        false

                override fun decode(
                    inputStream: Publisher<DataBuffer>,
                    elementType: ResolvableType,
                    mimeType: MimeType?,
                    hints: MutableMap<String, Any>?,
                ): Flux<Any> =
                        Flux.error(UnsupportedOperationException())
            }

    private fun noOpJsonEncoder(): AbstractEncoder<Any> =
            object : AbstractEncoder<Any>() {
                override fun canEncode(
                    elementType: ResolvableType,
                    mimeType: MimeType?,
                ): Boolean =
                        false

                override fun encode(
                    inputStream: Publisher<out Any>,
                    bufferFactory: DataBufferFactory,
                    elementType: ResolvableType,
                    mimeType: MimeType?,
                    hints: MutableMap<String, Any>?,
                ): Flux<DataBuffer> =
                        Flux.error(UnsupportedOperationException())
            }
}
