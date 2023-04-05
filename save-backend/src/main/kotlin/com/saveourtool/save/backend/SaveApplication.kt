package com.saveourtool.save.backend

import com.saveourtool.save.authservice.config.SecurityWebClientCustomizers
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.DefaultS3Configuration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.ParallelFlux
import java.nio.ByteBuffer

internal typealias FluxResponse<T> = ResponseEntity<Flux<T>>
internal typealias ParallelFluxResponse<T> = ResponseEntity<ParallelFlux<T>>
internal typealias ByteBufferFluxResponse = FluxResponse<ByteBuffer>

/**
 * An entrypoint for spring for save-backend
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
@Import(com.saveourtool.save.authservice.config.SecurityWebClientCustomizers::class, DefaultS3Configuration::class)
class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}
