package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.runner.SAVE_AGENT_USER_HOME
import com.saveourtool.save.orchestrator.service.PersistentVolumeService

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
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
    @Suppress("TOO_LONG_FUNCTION")
    override fun createFromResources(resources: Collection<Path>): DockerPvId {
        if (resources.size > 1) {
            TODO("Not yet implemented")
        }

        val createVolumeResponse = dockerClient.createVolumeCmd()
            .withName("save-execution-vol-${UUID.randomUUID()}")
            .exec()
        blockingPullImage("alpine", "latest")

        val resourcesRelativePath = resources.single().relativeTo(
            Paths.get(configProperties.testResources.tmpPath)
        )
        val intermediateResourcesPath = "$SAVE_AGENT_USER_HOME/tmp"
        val createContainerResponse = dockerClient.createContainerCmd("alpine:latest")
            .withHostConfig(
                HostConfig()
                    .withMounts(
                        listOf(
                            Mount()
                                .withType(MountType.VOLUME)
                                .withSource(configProperties.docker.testResourcesVolumeName)
                                .withTarget(intermediateResourcesPath),
                            Mount()
                                .withType(MountType.VOLUME)
                                .withSource(createVolumeResponse.name)
                                .withTarget(EXECUTION_DIR)
                        )
                    )
            )
            .withCmd(
                "sh", "-c",
                "cp -R $intermediateResourcesPath/${resourcesRelativePath.pathString}/* $EXECUTION_DIR" +
                        " && chown -R 1100:1100 $EXECUTION_DIR" +
                        " && echo Successfully copied"
            )
            .exec()
        val dataCopyingContainerId = createContainerResponse.id

        logger.info("Starting container $dataCopyingContainerId to copy files from $resources into volume ${createVolumeResponse.name}")
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

    @Suppress("MAGIC_NUMBER")
    private fun waitForCompletionWithTimeout(containerId: String) {
        val copyingTimeout = 200.seconds
        val checkInterval = 10.seconds
        Flux.interval(checkInterval.toJavaDuration())
            .take((copyingTimeout / checkInterval).toLong())
            .map { dockerClient.inspectContainerCmd(containerId).exec() }
            .filter { it.state.status != "running" }
            .blockFirst()
            .let { tick ->
                requireNotNull(tick) { "Container $containerId still running after $copyingTimeout" }
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerPersistentVolumeService::class.java)
    }
}
