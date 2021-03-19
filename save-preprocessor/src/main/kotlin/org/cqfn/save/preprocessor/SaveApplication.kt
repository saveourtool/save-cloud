package org.cqfn.save.preprocessor

import org.cqfn.save.preprocessor.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.config.EnableWebFlux
import reactor.core.publisher.Mono

/**
 * An entrypoint for spring for save-preprocessor
 */
@SpringBootApplication
@EnableWebFlux
@EnableConfigurationProperties(ConfigProperties::class)
class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}

typealias Response = Mono<ResponseEntity<String>>
