package com.saveourtool.save.gateway

import com.saveourtool.save.gateway.config.ConfigurationProperties

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Entrypoint for api-gateway spring boot application
 */
@EnableConfigurationProperties(ConfigurationProperties::class)
@SpringBootApplication
class SaveGateway

fun main(args: Array<String>) {
    SpringApplication.run(SaveGateway::class.java, *args)
}
