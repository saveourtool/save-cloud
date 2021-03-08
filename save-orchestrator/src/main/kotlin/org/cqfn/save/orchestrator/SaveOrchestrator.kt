package org.cqfn.save.orchestrator

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.web.reactive.config.EnableWebFlux

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@SpringBootApplication
@EnableWebFlux
open class SaveOrchestrator

fun main(args: Array<String>) {
    SpringApplication.run(SaveOrchestrator::class.java, *args)
}
