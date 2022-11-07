package com.saveourtool.save.demo

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration

/**
 * An entrypoint for spring boot for save-demo
 */
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class, XADataSourceAutoConfiguration::class])
open class SaveDemo

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemo::class.java, *args)
}
