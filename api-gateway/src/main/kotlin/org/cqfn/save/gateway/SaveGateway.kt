package org.cqfn.save.gateway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@EnableConfigurationProperties(ConfigurationProperties::class)
@SpringBootApplication
class SaveGateway

fun main(args: Array<String>) {
    SpringApplication.run(SaveGateway::class.java, *args)
}
