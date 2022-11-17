package com.saveourtool.save.demo.cpg

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories

/**
 * An entrypoint for spring boot for save-demo
 */
@SpringBootApplication
@EnableNeo4jRepositories
open class SaveDemoCpg

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemoCpg::class.java, *args)
}
