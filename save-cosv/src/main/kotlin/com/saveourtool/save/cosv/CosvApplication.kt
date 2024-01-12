package com.saveourtool.save.cosv

import com.saveourtool.save.cosv.configs.ConfigProperties
import com.saveourtool.save.s3.DefaultS3Configuration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

/**
 * An entrypoint for spring for save-cosv
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties::class)
@Import(
    DefaultS3Configuration::class,
)
class CosvApplication

fun main(args: Array<String>) {
    SpringApplication.run(CosvApplication::class.java, *args)
}
