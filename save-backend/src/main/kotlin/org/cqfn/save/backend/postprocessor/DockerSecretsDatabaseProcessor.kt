package org.cqfn.save.backend.postprocessor

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.context.annotation.Profile
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.FileSystemResource
import org.springframework.util.StreamUtils
import java.nio.charset.Charset
import java.util.Properties

/**
 * Post processor that collects credentials for database from docker secrets
 */
@Profile("prod")
class DockerSecretsDatabaseProcessor : EnvironmentPostProcessor {
    private val log = LoggerFactory.getLogger(DockerSecretsDatabaseProcessor::class.java)

    override fun postProcessEnvironment(environment: ConfigurableEnvironment?, application: SpringApplication?) {
        val secretsBasePath = System.getenv("DB_PASSWORD_FILE") ?: "/run/secrets"
        log.debug("Started DockerSecretsDatabaseProcessor [EnvironmentPostProcessor] configured to look up secrets in $secretsBasePath")
        val passwordResource = FileSystemResource("$secretsBasePath/db_password")
        val usernameResource = FileSystemResource("$secretsBasePath/db_username")
        val jdbcUrlResource = FileSystemResource("$secretsBasePath/db_url")

        if (passwordResource.exists()) {
            log.debug("Acquired password. Beginning to setting properties")
            val dbPassword = StreamUtils.copyToString(passwordResource.inputStream, Charset.defaultCharset())
            val dbUsername = StreamUtils.copyToString(usernameResource.inputStream, Charset.defaultCharset())
            val dbUrl = StreamUtils.copyToString(jdbcUrlResource.inputStream, Charset.defaultCharset())
            val props = Properties()
            props["spring.datasource.password"] = dbPassword
            props["spring.datasource.username"] = dbUsername
            props["spring.datasource.url"] = dbUrl
            environment?.propertySources?.addLast(PropertiesPropertySource("dbProps", props))
            log.debug("Properties have been set")
        }
    }
}
