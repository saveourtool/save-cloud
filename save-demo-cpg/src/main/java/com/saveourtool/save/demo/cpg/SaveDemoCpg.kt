package com.saveourtool.save.demo.cpg

import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * An entrypoint for spring boot for save-demo
 */
@SpringBootApplication
@EnableNeo4jRepositories
@EntityScan(basePackageClasses = [Component::class, Node::class])
@EnableTransactionManagement
open class SaveDemoCpg

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemoCpg::class.java, *args)
}
