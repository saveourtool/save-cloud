package org.cqfn.save.gateway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.web.reactive.config.EnableWebFlux

@EnableConfigurationProperties(ConfigurationProperties::class)
@SpringBootApplication
class SaveGateway

fun main(args: Array<String>) {
    SpringApplication.run(SaveGateway::class.java, *args)
}
