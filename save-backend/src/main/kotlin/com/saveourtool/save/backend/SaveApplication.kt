package com.saveourtool.save.backend

import com.saveourtool.save.backend.configs.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import java.nio.ByteBuffer

typealias ByteArrayResponse = ResponseEntity<ByteArray>
typealias StringResponse = ResponseEntity<String>
typealias EmptyResponse = ResponseEntity<Void>
typealias ResourceResponse = ResponseEntity<Resource>
typealias DataBufferFluxResponse = ResponseEntity<Flux<DataBuffer>>
typealias ByteBufferFluxResponse = ResponseEntity<Flux<ByteBuffer>>

/**
 * An entrypoint for spring for save-backend
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}
