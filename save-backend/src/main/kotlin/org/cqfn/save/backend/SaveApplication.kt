package org.cqfn.save.backend

import org.cqfn.save.backend.configs.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity
import springfox.documentation.oas.annotations.EnableOpenApi

typealias ByteArrayResponse = ResponseEntity<ByteArray>
typealias StringResponse = ResponseEntity<String>
typealias EmptyResponse = ResponseEntity<Void>

/**
 * An entrypoint for spring for save-backend
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}
