package com.saveourtool.save.demo

import com.saveourtool.save.demo.config.ConfigProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * An entrypoint for spring boot for save-demo
 */
@EnableConfigurationProperties(ConfigProperties::class)
@SpringBootApplication
@EntityScan(
    "com.saveourtool.save.demo.entity",
)
class SaveDemo

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemo::class.java, *args)
}
