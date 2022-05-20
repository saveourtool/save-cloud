package org.cqfn.save.orchestrator.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.LogConfig
import io.micrometer.core.instrument.MeterRegistry
import org.cqfn.save.orchestrator.DOCKER_METRIC_PREFIX
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.config.DockerSettings
import org.cqfn.save.orchestrator.createTgzStream
import org.cqfn.save.orchestrator.execTimed
import org.cqfn.save.orchestrator.getHostIp
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

@Component
@Profile("!kubernetes")
class DockerAgentRunner(
    configProperties: ConfigProperties,
    private val dockerClient: DockerClient,
    private val meterRegistry: MeterRegistry,
) : AgentRunner {
    private val settings: DockerSettings = requireNotNull(configProperties.docker) {
        "orchestrator.docker properties are missing but are required with current active profiles"
    }

    override fun create(
        executionId: Long,
        baseImageId: String,
        replicas: Int,
        workingDir: String,
        agentRunCmd: String,
    ): List<String> {
        return (1..replicas).map { number ->
            logger.info("Building container #$number for execution.id=${executionId}")
            createContainerFromImage(baseImageId, workingDir, agentRunCmd, containerName("${executionId}-$number")).also {
                logger.info("Built container id=$it for execution.id=${executionId}")
            }
        }
    }

    override fun start(id: String) {
        dockerClient.startContainerCmd(id).exec()
    }

    override fun stop(id: String) {
        dockerClient.stopContainerCmd(id).exec()
    }

    /**
     * Creates a docker container
     *
     * @param runCmd an entrypoint for docker container with CLI arguments
     * @param containerName a name for the created container
     * @param baseImageId id of the base docker image for this container
     * @param workingDir working directory for [runCmd]
     * @return id of created container or null if it wasn't created
     * @throws DockerException if docker daemon has returned an error
     * @throws RuntimeException if an exception not specific to docker has occurred
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    private fun createContainerFromImage(baseImageId: String,
                                          workingDir: String,
                                          runCmd: String,
                                          containerName: String,
    ): String {
        val baseImage = dockerClient.listImagesCmd().execTimed(meterRegistry, "$DOCKER_METRIC_PREFIX.image.list")!!.find {
            // fixme: sometimes createImageCmd returns short id without prefix, sometimes full and with prefix.
            it.id.replaceFirst("sha256:", "").startsWith(baseImageId.replaceFirst("sha256:", ""))
        }
            ?: error("Image with requested baseImageId=$baseImageId is not present in the system")
        // createContainerCmd accepts image name, not id, so we retrieve it from tags
        val createContainerCmdResponse = dockerClient.createContainerCmd(baseImage.repoTags.first())
            .withWorkingDir(workingDir)
            .withCmd("bash", "-c", "source .env && $runCmd")
            .withName(containerName)
            .withHostConfig(
                HostConfig.newHostConfig()
                .withRuntime(settings.runtime)
                // processes from inside the container will be able to access host's network using this hostname
                .withExtraHosts("host.docker.internal:${getHostIp()}")
                .withLogConfig(
                    when (settings.loggingDriver) {
                        "loki" -> LogConfig(
                            LogConfig.LoggingType.LOKI,
                            mapOf(
                                // similar to config in docker-compose.yaml
                                "mode" to "non-blocking",
                                "loki-url" to "http://127.0.0.1:9110/loki/api/v1/push",
                                "loki-external-labels" to "container_name={{.Name}},source=save-agent"
                            )
                        )
                        else -> LogConfig(LogConfig.LoggingType.DEFAULT)
                    }
                )
            )
            .execTimed(meterRegistry, "$DOCKER_METRIC_PREFIX.container.create")

        val containerId = createContainerCmdResponse!!.id
        val envFile = createTempDirectory("orchestrator").resolve(".env").apply {
            writeText("""
                AGENT_ID=$containerId""".trimIndent()
            )
        }
        copyResourcesIntoContainer(
            containerId,
            workingDir,
            listOf(envFile.toFile())
        )

        return containerId
    }

    /**
     * Copies specified [resources] into the container with id [containerId]
     *
     * @param resources additional resources
     * @param containerId id of the target container
     * @param remotePath path in the target container
     */
    internal fun copyResourcesIntoContainer(containerId: String,
                                            remotePath: String,
                                            resources: Collection<File>) {
        createTgzStream(*resources.toTypedArray()).use { out ->
            dockerClient.copyArchiveToContainerCmd(containerId)
                .withTarInputStream(out.toByteArray().inputStream())
                .withRemotePath(remotePath)
                .execTimed(meterRegistry, "$DOCKER_METRIC_PREFIX.container.copy.archive")
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(DockerAgentRunner::class.java)
    }
}

/**
 * @param id
 */
private fun containerName(id: String) = "save-execution-$id"
