package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.TerminateResponse
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.fillAgentPropertiesFromConfiguration
import com.saveourtool.save.orchestrator.runner.ContainerRunner
import com.saveourtool.save.orchestrator.runner.ContainerRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.utils.ContainersCollection
import com.saveourtool.save.request.RunExecutionRequest
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.warn

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.time.Duration.Companion.seconds

typealias AgentStateWithTimeStamp = Pair<String, Instant>

/**
 * A service that builds and starts containers for test execution.
 */
@Service
class ContainerService(
    private val configProperties: ConfigProperties,
    private val containerRunner: ContainerRunner,
    private val agentService: AgentService,
) {
    private val containers: ContainersCollection = ContainersCollection(configProperties.agentsHeartBeatTimeoutMillis)
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    /**
     * Collection that stores active containers using execution ID as group key for them
     */
    private val activeContainers: ConcurrentMap<Long, Set<String>> = ConcurrentHashMap()

    /**
     * Collection that stores the latest timestamp and state for each container
     */
    private val containerToLatestState: ConcurrentMap<String, AgentStateWithTimeStamp> = ConcurrentHashMap()
    /**
     * Collection that stores containers that are acting abnormally and will probably be terminated forcefully
     */
    private val crashedContainers: MutableSet<String> = ConcurrentHashMap.newKeySet()

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
                        duration.get() < configProperties.agentsStartTimeoutMillis && !isAnyContainerStartedForExecution(executionId)
                    }
                    .doOnNext {
                        duration.set((Clock.System.now() - now).inWholeMilliseconds)
                    }
                    .doOnComplete {
                        if (isAnyContainerStartedForExecution(executionId)) {
                            log.error("Internal error: none of agents $containerIds are started, will mark execution $executionId as failed.")
                            containerRunner.stop(executionId)
                            agentService.updateExecution(executionId, ExecutionStatus.ERROR,
                                "Internal error, raise an issue at https://github.com/saveourtool/save-cloud/issues/new"
                            ).then(agentService.markAllTestExecutionsOfExecutionAsFailed(executionId))
                                .subscribe()
                        }
                        activeContainers.remove(executionId)
                    }
            }
    }

    private fun isAnyContainerStartedForExecution(executionId: Long): Boolean {
        lock.readLock().lock()
        try {
            return activeContainers.getOrDefault(executionId, emptySet()).isNotEmpty()
        } finally {
            lock.readLock().unlock()
        }
    }

    /**
     * @param containerIds list of container IDs of agents to stop
     * @return true if agents have been stopped, false if another thread is already stopping them
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "FUNCTION_BOOLEAN_PREFIX")
    fun stopAgents(containerIds: Set<String>): Boolean {
        lock.writeLock().lock()
        try {
            activeContainers.keys.forEach { executionId ->
                activeContainers[executionId] = activeContainers[executionId]?.let {
                    it - containerIds
                }
            }
            containerToLatestState.keys.removeAll(containerIds)
            return try {
                containerIds.all { containerId ->
                    containerRunner.stopByContainerId(containerId)
                }
            } catch (e: ContainerRunnerException) {
                log.error("Error while stopping agents $containerIds", e)
                false
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    /**
     * @param executionId
     */
    fun touchContainer(
        executionId: Long,
        containerId: String,
        timestamp: Instant,
        state: AgentState,
    ) {
        containers.upsert(containerId, executionId, timestamp, state)
    }

    /**
     * Check whether the agent with [containerId] is stopped
     *
     * @param containerId id of an container
     * @return true if agent is stopped
     */
    fun isStoppedByContainerId(containerId: String): Boolean = containerRunner.isStoppedByContainerId(containerId)

    fun ensureGracefullyStopped(containerId: String) {
        val shutdownTimeoutSeconds = configProperties.shutdown.gracefulTimeoutSeconds.seconds
        val numChecks: Int = configProperties.shutdown.gracefulNumChecks
        Flux.interval((shutdownTimeoutSeconds / numChecks).toJavaDuration())
            .take(numChecks.toLong())
            .map {
                isStoppedByContainerId(containerId)
            }
            .takeUntil { it }
            // check whether we have got `true` or Flux has completed with only `false`
            .any { it }
            .doOnNext { successfullyStopped ->
                lock.writeLock().use {
                    if (!successfullyStopped) {
                        log.warn {
                            "Agent with containerId=$containerId is not stopped in $shutdownTimeoutSeconds seconds after ${TerminateResponse::class.simpleName} signal," +
                                    " will add it to crashed list"
                        }
                        crashedContainers.add(containerId)
                    } else {
                        log.debug { "Agent with containerId=$containerId has stopped after ${TerminateResponse::class.simpleName} signal" }
                        crashedContainers.remove(containerId)
                    }
                }

                // Update final execution status, perform cleanup etc.
                agentService.finalizeExecution(containerId)
            }
            .subscribeOn(agentService.scheduler)
            .subscribe()
    }
    /**
     * @param executionId ID of execution
     */
    fun cleanup(executionId: Long) {
        containers.deleteAllByExecutionId(executionId)
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
     * Consider agent as crashed, if it didn't send heartbeats for some time
     */
    fun determineCrashedAgents() {
        containers.recalculateCrashed()
        lock.writeLock().use {
            containerToLatestState.filter { (currentContainerId, _) ->
                currentContainerId !in crashedContainers
            }.forEach { (currentContainerId, stateToLatestHeartBeatPair) ->
                val duration = (Clock.System.now() - stateToLatestHeartBeatPair.second).inWholeMilliseconds
                log.debug {
                    "Latest heartbeat from $currentContainerId was sent: $duration ms ago"
                }
                if (duration >= configProperties.agentsHeartBeatTimeoutMillis) {
                    log.debug("Adding $currentContainerId to list crashed agents")
                    crashedContainers.add(currentContainerId)
                }
            }

            crashedContainers.removeIf { containerId ->
                isStoppedByContainerId(containerId)
            }
            containerToLatestState.filterKeys { containerId ->
                isStoppedByContainerId(containerId)
            }.forEach { (containerId, _) ->
                log.debug {
                    "Agent $containerId is already stopped, will stop watching it"
                }
                containerToLatestState.remove(containerId)
            }
        }
    }

    /**
     * Stop crashed agents and mark corresponding test executions as failed with internal error
     */
    fun processCrashedAgents() {
        if (crashedContainers.isEmpty()) {
            return
        }
        log.debug {
            "Stopping crashed agents: $crashedContainers"
        }

        val areAgentsStopped = stopAgents(crashedContainers)
        if (areAgentsStopped) {
            Flux.fromIterable(crashedContainers)
                .flatMap { containerId ->
                    agentService.updateAgentStatusesWithDto(AgentStatusDto(AgentState.CRASHED, containerId))
                }
                .blockLast()
            activeContainers
                .filter { (_, containerIds) -> containerIds.isEmpty() }
                .forEach { (executionId, _) ->
                    log.warn("All agents for execution $executionId are crashed, initialize cleanup for it.")
                    cleanup(executionId)
                    agentService.finalizeExecution(executionId)
                }
        } else {
            log.warn("Crashed agents $crashedContainers are not stopped after stop command")
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
        private val ILLEGAL_AGENT_STATES = setOf(AgentState.CRASHED, AgentState.TERMINATED, AgentState.STOPPED_BY_ORCH)
        internal const val SAVE_AGENT_EXECUTABLE_NAME = "save-agent.kexe"
    }
}

private fun <R> Lock.use(action: () -> R): R {
    lock()
    try {
        return action()
    } finally {
        unlock()
    }
}

/**
 * @param sdk
 * @return name like `save-base:openjdk-11`
 */
internal fun baseImageName(sdk: Sdk) = "ghcr.io/saveourtool/save-base:${sdk.toString().replace(":", "-")}"
