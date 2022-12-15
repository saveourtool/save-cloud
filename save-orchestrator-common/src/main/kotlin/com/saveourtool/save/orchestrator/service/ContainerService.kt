package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.fillAgentPropertiesFromConfiguration
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.runner.ContainerRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.request.RunExecutionRequest

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlinx.datetime.Clock

/**
 * A service that builds and starts containers for test execution.
 */
@Service
class ContainerService(
    private val configProperties: ConfigProperties,
    private val containerRunner: ContainerRunner,
    private val agentService: AgentService,
) {
    private val areAgentsHaveStarted: ConcurrentMap<Long, AtomicBoolean> = ConcurrentHashMap()

    /**
     * Function that builds a base image with test resources
     *
     * @param request [RunExecutionRequest] with info about [Execution] from which this workflow is started
     * @return image ID and execution command for the agent
     * @throws DockerException if interaction with docker daemon is not successful
     */
    @Suppress("UnsafeCallOnNullableType")
    fun prepareConfiguration(request: RunExecutionRequest): RunConfiguration {
        val buildResult = prepareConfigurationForExecution(request)
        log.info("For execution.id=${request.executionId} using base image [${buildResult.imageTag}]")
        return buildResult
    }

    /**
     * creates containers with agents
     *
     * @param executionId
     * @param configuration configuration for containers to be created
     * @return list of IDs of created containers
     */
    fun createContainers(
        executionId: Long,
        configuration: RunConfiguration,
    ) = containerRunner.create(
        executionId = executionId,
        configuration = configuration,
        replicas = configProperties.agentsCount,
    )

    /**
     * @param executionId ID of [Execution] for which containers are being started
     * @param containerIds list of IDs of agents (==containers) for this execution
     * @return Flux of ticks which correspond to attempts to check agents start, completes when agents are either
     * started or timeout is reached.
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun startContainersAndUpdateExecution(executionId: Long, containerIds: List<String>): Flux<Long> {
        log.info("Sending request to make execution.id=$executionId RUNNING")
        return agentService
            .updateExecution(executionId, ExecutionStatus.RUNNING)
            .map {
                containerRunner.start(executionId)
                log.info("Made request to start containers for execution.id=$executionId")
            }
            .flatMapMany {
                // Check, whether the agents were actually started, if yes, all cases will be covered by themselves and HeartBeatInspector,
                // if no, mark execution as failed with internal error here
                val now = Clock.System.now()
                val duration = AtomicLong(0)
                Flux.interval(configProperties.agentsStartCheckIntervalMillis.milliseconds.toJavaDuration())
                    .takeWhile {
                        val isAnyAgentStarted = areAgentsHaveStarted.computeIfAbsent(executionId) { AtomicBoolean(false) }.get()
                        duration.get() < configProperties.agentsStartTimeoutMillis && !isAnyAgentStarted
                    }
                    .doOnNext {
                        duration.set((Clock.System.now() - now).inWholeMilliseconds)
                    }
                    .doOnComplete {
                        if (areAgentsHaveStarted[executionId]?.get() != true) {
                            log.error("Internal error: none of agents $containerIds are started, will mark execution $executionId as failed.")
                            containerRunner.stop(executionId)
                            agentService.updateExecution(executionId, ExecutionStatus.ERROR,
                                "Internal error, raise an issue at https://github.com/saveourtool/save-cloud/issues/new"
                            ).then(agentService.markAllTestExecutionsOfExecutionAsFailed(executionId))
                                .subscribe()
                        }
                        areAgentsHaveStarted.remove(executionId)
                    }
            }
    }

    /**
     * @param containerIds list of container IDs of agents to stop
     * @return true if agents have been stopped, false if another thread is already stopping them
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "FUNCTION_BOOLEAN_PREFIX")
    fun stopAgents(containerIds: Collection<String>) =
            try {
                containerIds.all { containerId ->
                    containerRunner.stopByContainerId(containerId)
                }
            } catch (e: ContainerRunnerException) {
                log.error("Error while stopping agents $containerIds", e)
                false
            }

    /**
     * @param executionId
     */
    fun markAgentForExecutionAsStarted(executionId: Long) {
        areAgentsHaveStarted
            .computeIfAbsent(executionId) { AtomicBoolean(false) }
            .compareAndSet(false, true)
    }

    /**
     * Check whether the agent with [containerId] is stopped
     *
     * @param containerId id of an container
     * @return true if agent is stopped
     */
    fun isStoppedByContainerId(containerId: String): Boolean = containerRunner.isStoppedByContainerId(containerId)

    /**
     * @param executionId ID of execution
     */
    fun cleanup(executionId: Long) {
        containerRunner.cleanup(executionId)
    }

    private fun prepareConfigurationForExecution(request: RunExecutionRequest): RunConfiguration {
        val env = fillAgentPropertiesFromConfiguration(
            configProperties.agentSettings,
            request.saveAgentVersion,
            request.executionId,
        )

        val baseImage = baseImageName(request.sdk)
        return RunConfiguration(
            imageTag = baseImage,
            runCmd = listOf(
                "sh", "-c",
                "set -o xtrace" +
                        " && curl -vvv -X POST ${request.saveAgentUrl} --output $SAVE_AGENT_EXECUTABLE_NAME" +
                        " && chmod +x $SAVE_AGENT_EXECUTABLE_NAME" +
                        " && ./$SAVE_AGENT_EXECUTABLE_NAME"
            ),
            env = env,
        )
    }

    /**
     * Information required to start containers with save-agent
     *
     * @property imageTag tag of an image which should be used for a container
     * @property runCmd command that should be run as container's entrypoint.
     * Usually looks like `sh -c "rest of the command"`.
     * @property workingDir
     * @property env environment variables for the container
     */
    data class RunConfiguration(
        val imageTag: String,
        val runCmd: List<String>,
        val workingDir: String = EXECUTION_DIR,
        val env: Map<AgentEnvName, String>,
    )

    companion object {
        private val log = LoggerFactory.getLogger(ContainerService::class.java)
        internal const val SAVE_AGENT_EXECUTABLE_NAME = "save-agent.kexe"
    }
}

/**
 * @param sdk
 * @return name like `save-base:openjdk-11`
 */
internal fun baseImageName(sdk: Sdk) = "ghcr.io/saveourtool/save-base:${sdk.toString().replace(":", "-")}"
