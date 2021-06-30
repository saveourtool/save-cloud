package org.cqfn.save.orchestrator

import org.cqfn.save.orchestrator.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.config.EnableWebFlux

internal typealias BodilessResponseEntity = ResponseEntity<Void>

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@SpringBootApplication
@EnableWebFlux
@EnableConfigurationProperties(ConfigProperties::class)
open class SaveOrchestrator

fun main(args: Array<String>) {
    SpringApplication.run(SaveOrchestrator::class.java, *args)
}
