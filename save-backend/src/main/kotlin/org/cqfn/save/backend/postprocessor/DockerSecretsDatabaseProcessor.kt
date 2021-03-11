package org.cqfn.save.backend.postprocessor

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.context.annotation.Profile
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.FileSystemResource
import org.springframework.util.StreamUtils
import java.io.IOException
import java.lang.RuntimeException
import java.nio.charset.Charset
import java.util.*

@Profile("prod")
class DockerSecretsDatabaseProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(environment: ConfigurableEnvironment?, application: SpringApplication?) {
        val passwordResource = FileSystemResource("/run/secrets/db-password")
        val usernameResource = FileSystemResource("/run/secrets/db-username")

        if (passwordResource.exists()) {
            try {
                val dbPassword = StreamUtils.copyToString(passwordResource.inputStream, Charset.defaultCharset())
                val dbUsername = StreamUtils.copyToString(usernameResource.inputStream, Charset.defaultCharset())
                val props = Properties()
                props["spring.datasource.password"] = dbPassword
                props["spring.datasource.password"] = dbUsername
                environment?.propertySources?.addLast(PropertiesPropertySource("dbProps", props))
            } catch (exc: IOException) {
                throw RuntimeException(exc)
            }
        }
    }
}