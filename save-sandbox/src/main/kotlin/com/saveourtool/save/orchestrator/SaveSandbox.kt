package com.saveourtool.save.orchestrator

import com.saveourtool.save.orchestrator.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * An entrypoint for spring boot for save-sandbox
 */
@SpringBootApplication(
    scanBasePackages = [
        "com.saveourtool.save.sandbox",
        "com.saveourtool.save.orchestrator"
    ]
)
@EnableConfigurationProperties(ConfigProperties::class)
@EnableScheduling
open class SaveSandbox

fun main(args: Array<String>) {
    SpringApplication.run(SaveSandbox::class.java, *args)
}
