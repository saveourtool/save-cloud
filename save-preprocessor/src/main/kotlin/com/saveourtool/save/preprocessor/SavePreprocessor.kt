package com.saveourtool.save.preprocessor

import com.saveourtool.save.authservice.config.SecurityWebClientCustomizers
import com.saveourtool.save.preprocessor.config.ConfigProperties

import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.config.EnableWebFlux

/**
 * An entrypoint for spring for save-preprocessor
 */
@SpringBootApplication(exclude = [
    ReactiveSecurityAutoConfiguration::class,
    ReactiveUserDetailsServiceAutoConfiguration::class,
    ReactiveManagementWebSecurityAutoConfiguration::class,
])
@EnableWebFlux
@EnableConfigurationProperties(ConfigProperties::class)
@Import(SecurityWebClientCustomizers::class)
class SavePreprocessor

fun main(args: Array<String>) {
    SpringApplication.run(SavePreprocessor::class.java, *args)
}
