package com.saveourtool.save.preprocessor

import com.saveourtool.save.preprocessor.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.web.reactive.config.EnableWebFlux

/**
 * An entrypoint for spring for save-preprocessor
 */
@SpringBootApplication
@EnableWebFlux
@EnableConfigurationProperties(ConfigProperties::class)
class SavePreprocessor

fun main(args: Array<String>) {
    SpringApplication.run(SavePreprocessor::class.java, *args)
}
