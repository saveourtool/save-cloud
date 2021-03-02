package org.cqfn.save.preprocessor

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.reactive.config.EnableWebFlux

/**
 * An entrypoint for spring for save-preprocessor
 */
@SpringBootApplication
@EnableWebFlux
open class SaveApplication

fun main(args: Array<String>) {
    SpringApplication.run(SaveApplication::class.java, *args)
}
