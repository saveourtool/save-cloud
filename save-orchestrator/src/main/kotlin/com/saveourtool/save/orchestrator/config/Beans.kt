package com.saveourtool.save.orchestrator.config

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.saveourtool.save.orchestrator.docker.KubernetesManager
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.NamespacedKubernetesClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration class with various beans
 */
@Configuration
@Suppress("KDOC_WITHOUT_PARAM_TAG")
class Beans(private val configProperties: ConfigProperties) {
    /**
     * Used to send requests to backend
     *
     * @return [WebClient] with backend URL
     */
    @Bean
    fun webClientBackend() = WebClient.create(configProperties.backendUrl)

    /**
     * @param configProperties orchestrator configuration
     * @return instance of [DockerClient]
     */
    @Bean
    fun dockerClient(
        configProperties: ConfigProperties,
    ): DockerClient {
        val settings = requireNotNull(configProperties.docker) {
            "properties `orchestrator.docker.*` are required to setup docker client"
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
    @Bean
    @Profile("kubernetes")
    fun kubernetesClient(configProperties: ConfigProperties): KubernetesClient {
        val kubernetesSettings = requireNotNull(configProperties.kubernetes) {
            "Class [${KubernetesManager::class.simpleName}] requires `orchestrator.kubernetes.*` properties to be set"
        }

        return DefaultKubernetesClient().inNamespace(kubernetesSettings.namespace)
    }
}
