package com.saveourtool.save.demo.cpg

import com.saveourtool.save.demo.cpg.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity

/**
 * An entrypoint for spring boot for save-demo
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
class SaveDemoCpg

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemoCpg::class.java, *args)
}
