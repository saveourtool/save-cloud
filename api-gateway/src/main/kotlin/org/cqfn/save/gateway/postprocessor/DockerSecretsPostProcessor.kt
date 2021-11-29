package org.cqfn.save.gateway.postprocessor

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
 * Post processor that collects credentials for keycloak from docker secrets
 */
@Profile("prod")
class DockerSecretsPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val keycloakClientSecretFile = FileSystemResource("/run/secrets/keycloak_gw_secret")

        if (keycloakClientSecretFile.exists()) {
            val keycloakClientSecret = StreamUtils.copyToString(keycloakClientSecretFile.inputStream, Charset.defaultCharset())
            val props = Properties()
            props["spring.security.oauth2.client.registration.keycloak.client-secret"] = keycloakClientSecret
            environment.propertySources.addLast(PropertiesPropertySource("kc_props", props))
        }
    }
}
