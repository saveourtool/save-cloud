/**
 * save-demo configuration
 */

package com.saveourtool.save.demo.cpg.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property authentication
 * @property uri
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "spring.neo4j")
data class ConfigProperties(
    val authentication: Authentication,
    var uri: String
) {
    /**
     * @property username
     * @property password
     */
    data class Authentication(
        var username: String,
        var password: String,
    )
}
