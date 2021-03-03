package org.cqfn.save.backend

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.config.EnableWebFlux
import reactor.core.publisher.Mono

/**
 * An entrypoint for spring for save-backend
 */
@SpringBootApplication
@EnableWebFlux
@EnableJpaRepositories(basePackages = ["org.cqfn.save.backend.repository"])
@EntityScan("org.cqfn.save.entities")
open class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}

typealias Response = Mono<ResponseEntity<String>>
