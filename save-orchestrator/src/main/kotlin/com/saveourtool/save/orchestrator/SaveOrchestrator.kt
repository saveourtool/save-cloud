package com.saveourtool.save.orchestrator

import com.saveourtool.save.authservice.config.SecurityWebClientCustomizers
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@SpringBootApplication
@Import(SecurityWebClientCustomizers::class)
class SaveOrchestrator

fun main(args: Array<String>) {
    SpringApplication.run(SaveOrchestrator::class.java, *args)
}
