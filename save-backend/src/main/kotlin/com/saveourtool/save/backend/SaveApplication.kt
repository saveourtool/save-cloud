package com.saveourtool.save.backend

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.cosv.CosvConfiguration
import com.saveourtool.save.s3.DefaultS3Configuration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring for save-backend
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
@Import(
    DefaultS3Configuration::class,
    CosvConfiguration::class,
)
class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}
