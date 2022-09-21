package com.saveourtool.save.sandbox

import com.saveourtool.save.orchestrator.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableScheduling

internal typealias BodilessResponseEntity = ResponseEntity<Void>

/**
 * An entrypoint for spring boot for save-sandbox
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
@EnableScheduling
open class SaveSandbox

fun main(args: Array<String>) {
    SpringApplication.run(SaveSandbox::class.java, *args)
}
