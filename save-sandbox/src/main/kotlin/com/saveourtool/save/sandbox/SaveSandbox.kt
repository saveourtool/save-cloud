package com.saveourtool.save.sandbox

import com.saveourtool.save.authservice.config.SecurityWebClientCustomizers
import com.saveourtool.save.s3.DefaultS3Configuration
import com.saveourtool.save.sandbox.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring boot for save-sandbox
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
@Import(SecurityWebClientCustomizers::class, DefaultS3Configuration::class)
@EntityScan(
    "com.saveourtool.save.entities",
    "com.saveourtool.save.sandbox.entity",
)
open class SaveSandbox

fun main(args: Array<String>) {
    SpringApplication.run(SaveSandbox::class.java, *args)
}
