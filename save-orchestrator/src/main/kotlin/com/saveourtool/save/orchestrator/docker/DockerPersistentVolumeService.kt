package com.saveourtool.save.orchestrator.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.saveourtool.save.orchestrator.service.PersistentVolumeId
import com.saveourtool.save.orchestrator.service.PersistentVolumeService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Component
@Profile("!kubernetes")
class DockerPersistentVolumeService(
    private val dockerClient: DockerClient,
) : PersistentVolumeService {
    override fun createFromResources(resources: Collection<Path>): DockerPvId {
        if (resources.size > 1) {
            TODO("Not yet implemented")
        }

        val createVolumeResponse = dockerClient.createVolumeCmd()
            .withName("save-execution-vol-${UUID.randomUUID()}")
            .exec()
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
        Flux.interval(10.seconds.toJavaDuration())
            .take(20)
            .takeWhile {
                val inspectContainerResponse = dockerClient.inspectContainerCmd(dataCopyingContainerId).exec()
                inspectContainerResponse.state.status == "running"
            }
            .blockLast()

        dockerClient.removeContainerCmd(dataCopyingContainerId)

        return DockerPvId(createVolumeResponse.name)
    }
}

data class DockerPvId(
    val volumeName: String,
) : PersistentVolumeId
