package com.saveourtool.save.orchestrator

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@SpringBootApplication
open class SaveOrchestrator

fun main(args: Array<String>) {
    SpringApplication.run(SaveOrchestrator::class.java, *args)
}
