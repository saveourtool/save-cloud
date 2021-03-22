package org.cqfn.save.backend

import org.cqfn.save.backend.configs.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.web.reactive.config.EnableWebFlux

/**
 * An entrypoint for spring for save-backend
 */
@SpringBootApplication
@EnableWebFlux
@EnableJpaRepositories(basePackages = ["org.cqfn.save.backend.repository"])
@EntityScan("org.cqfn.save.entities")
@EnableConfigurationProperties(ConfigProperties::class)
open class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}
