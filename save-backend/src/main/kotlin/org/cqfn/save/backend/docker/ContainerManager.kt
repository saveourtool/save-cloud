package org.cqfn.save.backend.docker

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient

class ContainerManager {
    private val unixSocketConfig: DockerClientConfig = DefaultDockerClientConfig
        .createDefaultConfigBuilder()
        .build()
    private val dockerHttpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
        .dockerHost(unixSocketConfig.dockerHost)
        .build()
    private val dockerClient = DockerClientImpl.getInstance(unixSocketConfig, dockerHttpClient)

    fun foo() {
        dockerClient.createContainerCmd("ubuntu:latest")
            .withHostConfig(HostConfig.newHostConfig()
                .withRuntime("runsc")
            )
            .exec()
    }
}
