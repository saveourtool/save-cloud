package com.saveourtool.save.orchestrator.config

import com.saveourtool.save.orchestrator.kubernetes.KubernetesManager

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Configuration class with various beans
 */
@Configuration
@Suppress("KDOC_WITHOUT_PARAM_TAG")
class Beans {
    /**
     * @param configProperties orchestrator configuration
     * @return instance of [DockerClient]
     */
    @Bean
    @Profile("!kubernetes && !docker-test")
    fun dockerClient(
        configProperties: ConfigProperties,
    ): DockerClient {
        val settings = requireNotNull(configProperties.docker) {
            "Properties under configProperties.docker are not set, but are required with active profiles."
        }
        val dockerClientConfig: DockerClientConfig = DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .withDockerHost(settings.host)
            .withDockerTlsVerify(false)
            .build()
        val dockerHttpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.dockerHost)
            .build()

        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient)
    }

    /**
     * @return a Kubernetes client that uses properties from [configProperties] for connection
     */
    @Bean(destroyMethod = "close")
    @Profile("kubernetes")
    fun kubernetesClient(configProperties: ConfigProperties): KubernetesClient {
        val kubernetesSettings = requireNotNull(configProperties.kubernetes) {
            "Class [${KubernetesManager::class.simpleName}] requires `orchestrator.kubernetes.*` properties to be set"
        }

        return KubernetesClientBuilder()
            .withConfig(ConfigBuilder()
                .withNamespace(kubernetesSettings.namespace)
                .build())
            .build()
    }
}
