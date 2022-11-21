/**
 * save-demo configuration
 */

package com.saveourtool.save.demo.cpg.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration

/**
 * @property fileStorage
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "spring.neo4j")
data class ConfigProperties(
    val authentication: Authentication,
    var uri: String
)

@ConfigurationProperties(prefix = "authentication")
data class Authentication(
    var username: String,
    var password: String,
)