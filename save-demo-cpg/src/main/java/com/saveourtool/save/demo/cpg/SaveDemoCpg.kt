package com.saveourtool.save.demo.cpg

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * An entrypoint for spring boot for save-demo
 */
@SpringBootApplication
open class SaveDemoCpg

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemoCpg::class.java, *args)
}
