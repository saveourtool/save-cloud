package com.saveourtool.save.demo.config

import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Class for [KubernetesClient] bean initialization
 */
@Configuration
class Beans {
    /**
     * @param configProperties application configuration
     * @return configured [KubernetesClient]
     */
    @Bean(destroyMethod = "close")
    fun kubernetesClient(configProperties: ConfigProperties): KubernetesClient {
        val kubernetesSettings = configProperties.kubernetes
        return KubernetesClientBuilder()
            .withConfig(ConfigBuilder()
                .withNamespace(kubernetesSettings.namespace)
                .build())
            .build()
    }
}
