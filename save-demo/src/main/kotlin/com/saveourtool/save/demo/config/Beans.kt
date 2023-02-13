package com.saveourtool.save.demo.config

import com.saveourtool.save.demo.service.KubernetesService
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Beans {
    /**
     * @param configProperties
     * @return
     */
    @Bean(destroyMethod = "close")
    fun kubernetesClient(configProperties: ConfigProperties): KubernetesClient {
        val kubernetesSettings = configProperties.kubernetes
        return DefaultKubernetesClient().inNamespace(kubernetesSettings.namespace)
    }
}
