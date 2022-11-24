package com.saveourtool.save.backend

import com.saveourtool.save.authservice.config.SecurityWebClientCustomizers
import com.saveourtool.save.backend.configs.ConfigProperties

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import java.nio.ByteBuffer

typealias ByteArrayResponse = ResponseEntity<ByteArray>
typealias StringResponse = ResponseEntity<String>
typealias EmptyResponse = ResponseEntity<Void>
typealias ByteBufferFluxResponse = ResponseEntity<Flux<ByteBuffer>>
typealias ResourceResponse = ResponseEntity<Resource>
typealias StringList = List<String>

/**
 * An entrypoint for spring for save-backend
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
@org.springframework.context.annotation.Import(com.saveourtool.save.authservice.config.SecurityWebClientCustomizers::class)
class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}
