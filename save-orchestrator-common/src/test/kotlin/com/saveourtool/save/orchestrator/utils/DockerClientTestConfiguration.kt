package com.saveourtool.save.orchestrator.utils

import com.github.dockerjava.api.DockerClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.testcontainers.DockerClientFactory

/**
 * Override [DockerClient] by docker client from testcontainers
 */
@TestConfiguration
class DockerClientTestConfiguration {
    /**
     * @return [DockerClient]
     */
    @Bean
    fun dockerClient(): DockerClient = DockerClientFactory.lazyClient()
}
