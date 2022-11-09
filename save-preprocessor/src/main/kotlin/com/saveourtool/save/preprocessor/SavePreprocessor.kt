package com.saveourtool.save.preprocessor

import com.saveourtool.save.configs.WebClientCustomizers
import com.saveourtool.save.preprocessor.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.config.EnableWebFlux

typealias TextResponse = ResponseEntity<String>
typealias EmptyResponse = ResponseEntity<Void>
typealias StatusResponse = ResponseEntity<HttpStatus>

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
@Import(WebClientCustomizers::class)
class SavePreprocessor

fun main(args: Array<String>) {
    SpringApplication.run(SavePreprocessor::class.java, *args)
}
