package com.saveourtool.save.sandbox

import com.saveourtool.save.authservice.config.SecurityWebClientCustomizers
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring boot for save-sandbox
 */
@SpringBootApplication
@Import(SecurityWebClientCustomizers::class)
class SaveSandbox

fun main(args: Array<String>) {
    SpringApplication.run(SaveSandbox::class.java, *args)
}
