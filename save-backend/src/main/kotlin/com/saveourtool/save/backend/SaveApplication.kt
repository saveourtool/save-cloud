package com.saveourtool.save.backend

import com.saveourtool.save.backend.configs.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity

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
