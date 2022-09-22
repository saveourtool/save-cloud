package com.saveourtool.save.orchestrator

import com.saveourtool.save.orchestrator.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling

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
