package org.cqfn.save.orchestrator

import org.cqfn.save.orchestrator.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling

internal typealias BodilessResponseEntity = ResponseEntity<Void>
internal typealias TextResponse = ResponseEntity<String>

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
@EnableScheduling
open class SaveOrchestrator

fun main(args: Array<String>) {
    SpringApplication.run(SaveOrchestrator::class.java, *args)
}
