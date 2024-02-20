package com.saveourtool.cosv.backend

import com.saveourtool.cosv.backend.configs.ConfigProperties
import com.saveourtool.save.s3.DefaultS3Configuration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring for cosv-backend
 */
@SpringBootApplication(scanBasePackages = [
    "com.saveourtool.save.configs",
    "com.saveourtool.cosv.backend",
    "com.saveourtool.save.service",
    "com.saveourtool.save.storage",
    "com.saveourtool.save.security",
    "com.saveourtool.save.utils",
    "com.saveourtool.save.repository",
    "com.saveourtool.save.authservice",
])
@EnableConfigurationProperties(ConfigProperties::class)
@Import(
    DefaultS3Configuration::class,
)
@EntityScan(basePackages = ["com.saveourtool.save.entities", "com.saveourtool.save.entitiescosv"])
class CosvApplication

fun main(args: Array<String>) {
    SpringApplication.run(CosvApplication::class.java, *args)
}
