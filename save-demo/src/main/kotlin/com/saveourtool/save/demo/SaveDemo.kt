package com.saveourtool.save.demo

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.s3.DefaultS3Configuration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity

/**
 * An entrypoint for spring boot for save-demo
 */
@EnableConfigurationProperties(ConfigProperties::class)
@Import(DefaultS3Configuration::class)
@SpringBootApplication
class SaveDemo

fun main(args: Array<String>) {
    SpringApplication.run(SaveDemo::class.java, *args)
}