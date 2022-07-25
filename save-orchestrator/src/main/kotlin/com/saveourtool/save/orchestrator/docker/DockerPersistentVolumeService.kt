package com.saveourtool.save.orchestrator.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.service.PersistentVolumeService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Implementation of [PersistentVolumeService] that creates [Docker Volumes](https://docs.docker.com/storage/volumes/)
 */
@Component
@Profile("!kubernetes")
class DockerPersistentVolumeService(
    private val dockerClient: DockerClient,
    private val configProperties: ConfigProperties,
) : PersistentVolumeService {
    override fun createFromResources(resources: Collection<Path>): DockerPvId {
        if (resources.size > 1) {
            TODO("Not yet implemented")
        }

        val createVolumeResponse = dockerClient.createVolumeCmd()
            .withName("save-execution-vol-${UUID.randomUUID()}")
            .exec()
        blockingPullImage("alpine", "latest")
        val createContainerResponse = dockerClient.createContainerCmd("alpine:latest")
            .withHostConfig(
                HostConfig()
                    .withMounts(
                        listOf(
                            Mount()
                                .withType(MountType.BIND)
                                .withSource(resources.single().absolutePathString())
                                .withTarget("/home/save-agent/tmp"),
                            Mount()
                                .withType(MountType.VOLUME)
                                .withSource(createVolumeResponse.name)
                                .withTarget("/home/save-agent/save-execution")
                        )
                    )
            )
            .withCmd(
                "sh", "-c",
                "cp -R /home/save-agent/tmp/* /home/save-agent/save-execution && chown -R 1100:1100 /home/save-agent/save-execution" +
                        " && echo Successfully copied"
            )
            .exec()
        val dataCopyingContainerId = createContainerResponse.id

        dockerClient.startContainerCmd(dataCopyingContainerId)
            .exec()
        waitForCompletionWithTimeout(dataCopyingContainerId)

        dockerClient.removeContainerCmd(dataCopyingContainerId)

        return DockerPvId(createVolumeResponse.name)
    }

    private fun blockingPullImage(
        repository: String,
        tag: String
    ) = dockerClient.pullImageCmd(repository)
        .withRegistry(configProperties.docker.registry)
        .withTag(tag)
        .exec(PullImageResultCallback())
        .awaitCompletion()

    private fun waitForCompletionWithTimeout(containerId: String) {
        val copyingTimeout = 200.seconds
        val checkInterval = 10.seconds
        Flux.interval(checkInterval.toJavaDuration())
            .take((copyingTimeout / checkInterval).toLong())
            .takeUntil {
                val inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec()
                inspectContainerResponse.state.status == "running"
            }
            .blockLast()
            .let {
                requireNotNull(it) { "Container $containerId still running after $copyingTimeout" }
            }
    }
}

