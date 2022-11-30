package com.saveourtool.save.demo.cpg

import com.saveourtool.save.demo.cpg.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.http.ResponseEntity

typealias StringResponse = ResponseEntity<String>

/**
 * An entrypoint for spring boot for save-demo
 */
@SpringBootApplication
@EnableNeo4jRepositories

@EnableConfigurationProperties(ConfigProperties::class)
class SaveDemoCpg

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemoCpg::class.java, *args)
}
