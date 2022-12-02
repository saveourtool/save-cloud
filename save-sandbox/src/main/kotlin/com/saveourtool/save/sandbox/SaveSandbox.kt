package com.saveourtool.save.sandbox

import com.saveourtool.save.sandbox.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * An entrypoint for spring boot for save-sandbox
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
open class SaveSandbox

fun main(args: Array<String>) {
    SpringApplication.run(SaveSandbox::class.java, *args)
}
