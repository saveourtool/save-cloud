package com.saveourtool.save.orchestrator.sandbox

import com.saveourtool.save.orchestrator.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling

const val SANDBOX_PROFILE = "sandbox"

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
@EnableScheduling
open class SandboxSaveOrchestrator

fun main(args: Array<String>) {
    val springApplication = SpringApplication(SandboxSaveOrchestrator::class.java)
    springApplication.setAdditionalProfiles(SANDBOX_PROFILE)
    springApplication.run(*args)
}
