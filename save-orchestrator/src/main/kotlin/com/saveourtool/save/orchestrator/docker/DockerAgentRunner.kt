package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.orchestrator.*
import com.saveourtool.save.orchestrator.DOCKER_METRIC_PREFIX
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.config.ConfigProperties.DockerSettings
import com.saveourtool.save.orchestrator.createTgzStream
import com.saveourtool.save.orchestrator.runner.AgentRunner
import com.saveourtool.save.orchestrator.runner.AgentRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.runner.SAVE_AGENT_USER_HOME
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.PersistentVolumeId
import com.saveourtool.save.utils.debug

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.model.*
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

/**
 * [AgentRunner] that uses Docker Daemon API to run save-agents
 */
@Component
@Profile("!kubernetes")
class DockerAgentRunner(
    private val configProperties: ConfigProperties,
    private val dockerClient: DockerClient,
    private val meterRegistry: MeterRegistry,
) : AgentRunner {
    private val settings: DockerSettings = requireNotNull(configProperties.docker) {
        "Properties under configProperties.docker are not set, but are required with active profiles."
    }

    @Suppress("TYPE_ALIAS")
    private val agentIdsByExecution: ConcurrentMap<Long, MutableList<String>> = ConcurrentHashMap()

    override fun create(
        executionId: Long,
        configuration: DockerService.RunConfiguration<PersistentVolumeId>,
        replicas: Int,
    ): List<String> {
        require(configuration.pvId is DockerPvId) { "${DockerPersistentVolumeService::class.simpleName} can only operate with ${DockerPvId::class.simpleName}" }

        logger.debug { "Pulling image ${configuration.imageTag}" }
        dockerClient.pullImageCmd(configuration.imageTag)
            .withRegistry("https://ghcr.io")
            .exec(PullImageResultCallback())
            .awaitCompletion()

        return (1..replicas).map { number ->
            logger.info("Creating a container #$number for execution.id=$executionId")
            createContainerFromImage(configuration as DockerService.RunConfiguration<DockerPvId>, containerName("$executionId-$number")).also { agentId ->
                logger.info("Created a container id=$agentId for execution.id=$executionId")
                agentIdsByExecution
                    .getOrPut(executionId) { mutableListOf() }
                    .add(agentId)
            }
        }
    }

    override fun start(executionId: Long) {
        val agentIds = agentIdsByExecution.computeIfAbsent(executionId) {
            // For executions started by the running instance of orchestrator, this key should be already present in the map.
            // Otherwise, it will be added by `DockerAgentRunner#discover`, which is not yet implemented.
            TODO("${DockerAgentRunner::class.simpleName} should be able to load data about agents started by other instances of orchestrator")
        }
        agentIds.forEach { agentId ->
            logger.info("Starting container id=$agentId")
            dockerClient.startContainerCmd(agentId).exec()
        }
    }

    override fun stop(executionId: Long) {
        val runningContainersForExecution = dockerClient.listContainersCmd()
            .withStatusFilter(listOf("running"))
            .exec()
            .filter { container -> container.names.any { it.contains("-$executionId-") } }
        runningContainersForExecution.map { it.id }.forEach { agentId ->
            dockerClient.stopContainerCmd(agentId).exec()
        }
    }

    override fun stopByAgentId(agentId: String): Boolean {
        logger.info("Stopping agent with id=$agentId")
        val state = dockerClient.inspectContainerCmd(agentId).exec().state
        return if (state.status == "running") {
            try {
                dockerClient.stopContainerCmd(agentId).exec()
            } catch (dex: DockerException) {
                throw AgentRunnerException("Exception when stopping agent id=$agentId", dex)
            }
            logger.info("Agent with id=$agentId has been stopped")
            true
        } else {
            if (state.status != "exited") {
                logger.warn("Agent with id=$agentId was requested to be stopped, but it actual state=$state")
            }
            state.status == "exited"
        }
    }

    override fun isAgentStopped(agentId: String): Boolean = dockerClient.inspectContainerCmd(agentId)
        .exec()
        .state
        .also { logger.debug("Container $agentId has state $it") }
        .status != "running"

    override fun cleanup(executionId: Long) {
        val containersForExecution = dockerClient.listContainersCmd().withNameFilter(listOf("-$executionId-")).exec()

        containersForExecution.map { it.id }.forEach { containerId ->
            logger.info("Removing container $containerId")
            val existingContainerIds = dockerClient.listContainersCmd().withShowAll(true).exec()
                .map {
                    it.id
                }
            if (containerId in existingContainerIds) {
                dockerClient.removeContainerCmd(containerId).exec()
            } else {
                logger.info("Container $containerId is not present, so won't attempt to remove")
            }
        }
    }

    @Scheduled(cron = "0 0 4 * * MON")
    override fun prune() {
        var reclaimedBytes = 0L
        // Release all old resources, except volumes,
        // since there is no option --filter for `docker volume prune`, and also it could be quite dangerous to remove volumes,
        // as it possible to lose some prepared data
        for (type in PruneType.values().filterNot { it == PruneType.VOLUMES }) {
            val pruneCmd = dockerClient.pruneCmd(type).withUntilFilter(configProperties.dockerResourcesLifetime).exec()
            val currentReclaimedBytes = pruneCmd.spaceReclaimed ?: 0
            logger.debug("Reclaimed $currentReclaimedBytes bytes after prune of docker $type")
            reclaimedBytes += currentReclaimedBytes
        }
        logger.info("Reclaimed $reclaimedBytes bytes after prune command")
    }

    /**
     * Creates a docker container
     *
     * @param runCmd an entrypoint for docker container with CLI arguments
     * @param containerName a name for the created container
     * @param baseImageTag tag of the base docker image for this container
     * @param workingDir working directory for [runCmd]
     * @return id of created container or null if it wasn't created
     * @throws DockerException if docker daemon has returned an error
     * @throws RuntimeException if an exception not specific to docker has occurred
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    private fun createContainerFromImage(configuration: DockerService.RunConfiguration<DockerPvId>,
                                         containerName: String,
    ): String {
        val baseImageTag = configuration.imageTag
        val pvId = configuration.pvId
        val runCmd = configuration.runCmd
        val envFileTargetPath = "$SAVE_AGENT_USER_HOME/.env"
        // createContainerCmd accepts image name, not id, so we retrieve it from tags
        val createContainerCmdResponse: CreateContainerResponse = dockerClient.createContainerCmd(baseImageTag)
            .withWorkingDir(EXECUTION_DIR)
            // Load environment variables required by save-agent and then run it.
            // Rely on `runCmd` format: last argument is parameter of the subshell.
            .withCmd(
                // this part is like `sh -c` with probably some other flags
                runCmd.dropLast(1) + (
                        // last element is an actual command that will be executed in a new shell
                        """env $(cat $envFileTargetPath | xargs) sh -c "cp $SAVE_AGENT_USER_HOME/resources/* . && ${runCmd.last()}""""
                )
            )
            .withName(containerName)
            .withUser("save-agent")
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withBinds(Bind(
                        // Apparently, target path needs to be wrapped into [Volume] object in Docker API.
                        pvId.volumeName, Volume("$SAVE_AGENT_USER_HOME/resources")
                    ))
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

        val containerId = createContainerCmdResponse.id
        val envFile = createTempDirectory("orchestrator").resolve(".env").apply {
            writeText("""
                AGENT_ID=$containerId
                EXECUTION_ID=${configuration.resourcesConfiguration.executionId}
                ADDITIONAL_FILES_LIST=${configuration.resourcesConfiguration.additionalFilesString}
                ORGANIZATION_NAME=${configuration.resourcesConfiguration.organizationName}
                PROJECT_NAME=${configuration.resourcesConfiguration.projectName}
                """.trimIndent()
            )
        }
        copyResourcesIntoContainer(
            containerId,
            envFileTargetPath.substringBeforeLast("/"),
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
                .execTimed<CopyArchiveToContainerCmd, Void?>(meterRegistry, "$DOCKER_METRIC_PREFIX.container.copy.archive")
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
