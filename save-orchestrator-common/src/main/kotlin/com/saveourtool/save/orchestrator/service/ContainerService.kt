package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.fillAgentPropertiesFromConfiguration
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.utils.AgentStatusInMemoryRepository
import com.saveourtool.save.request.RunExecutionRequest
import com.saveourtool.save.utils.waitReactivelyUntil
import com.saveourtool.save.utils.warn

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * A service that builds and starts containers for test execution.
 */
@Service
class ContainerService(
    private val configProperties: ConfigProperties,
    private val containerRunner: ContainerRunner,
    private val agentService: AgentService,
    private val agentStatusInMemoryRepository: AgentStatusInMemoryRepository,
) {
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
    ): List<String> = containerRunner.create(
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
    fun startContainersAndUpdateExecution(executionId: Long, containerIds: List<String>): Mono<Boolean> {
        log.info("Sending request to make execution.id=$executionId RUNNING")
        return agentService
            .updateExecution(executionId, ExecutionStatus.RUNNING)
            .map {
                containerRunner.startAllByExecution(executionId)
                log.info("Made request to start containers for execution.id=$executionId")
            }
            .flatMap {
                // Check, whether the agents were actually started, if yes, all cases will be covered by themselves and HeartBeatInspector,
                // if no, mark execution as failed with internal error here
                waitReactivelyUntil(
                    interval = configProperties.agentsStartCheckIntervalMillis.milliseconds,
                    numberOfChecks = configProperties.agentsStartTimeoutMillis / configProperties.agentsStartCheckIntervalMillis
                ) {
                    !agentStatusInMemoryRepository.containsAnyByExecutionId(executionId)
                }
                    .doOnSuccess {
                        if (!agentStatusInMemoryRepository.containsAnyByExecutionId(executionId)) {
                            log.error("Internal error: none of agents $containerIds are started, will mark execution $executionId as failed.")
                            containerRunner.cleanupAllByExecution(executionId)
                            agentService.updateExecution(executionId, ExecutionStatus.ERROR,
                                "Internal error, raise an issue at https://github.com/saveourtool/save-cloud/issues/new"
                            ).then(agentService.markAllTestExecutionsOfExecutionAsFailed(executionId))
                                .subscribe()
                        }
                        agentStatusInMemoryRepository.deleteAllByExecutionId(executionId)
                    }
            }
    }

    /**
     * Check whether the agent with [containerId] is stopped
     *
     * @param containerId id of an container
     * @return true if agent is stopped
     */
    fun isStopped(containerId: String): Boolean = containerRunner.isStopped(containerId)

    /**
     * @param executionId ID of execution
     */
    fun cleanupAllByExecution(executionId: Long) {
        agentStatusInMemoryRepository.deleteAllByExecutionId(executionId)
        containerRunner.cleanupAllByExecution(executionId)
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

    @Scheduled(cron = "*/\${orchestrator.heart-beat-inspector-interval} * * * * ?")
    private fun run() {
        determineCrashedAgents()
        cleanupExecutionWithoutContainers()
    }

    /**
     * Consider agent as crashed, if it didn't send heartbeats for some time
     */
    private fun determineCrashedAgents() {
        agentStatusInMemoryRepository.updateByStatus { containerId -> isStopped(containerId) }
    }

    /**
     * Stop crashed agents and mark corresponding test executions as failed with internal error
     */
    private fun cleanupExecutionWithoutContainers() {
        agentStatusInMemoryRepository.processExecutionWithAllCrashedContainers { executionIds ->
            executionIds.forEach { executionId ->
                log.warn("All agents for execution $executionId are crashed or not started, initialize cleanup for it.")
                agentStatusInMemoryRepository.deleteAllByExecutionId(executionId)
                agentService.finalizeExecution(executionId)
            }
        }
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
