package com.saveourtool.save.sandbox

import com.saveourtool.save.authservice.config.SecurityWebClientCustomizers
import com.saveourtool.save.sandbox.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring boot for save-sandbox
 */
@SpringBootApplication
@Import(SecurityWebClientCustomizers::class)
@EnableConfigurationProperties(ConfigProperties::class)
open class SaveSandbox

fun main(args: Array<String>) {
    SpringApplication.run(SaveSandbox::class.java, *args)
}
