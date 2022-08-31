package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.fillAgentPropertiesFromConfiguration
import com.saveourtool.save.orchestrator.runner.AgentRunner
import com.saveourtool.save.orchestrator.runner.AgentRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

import java.util.concurrent.atomic.AtomicLong

import kotlin.io.path.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlinx.datetime.Clock

/**
 * A service that builds and starts containers for test execution.
 */
@Service
@OptIn(ExperimentalPathApi::class)
class DockerService(
    private val configProperties: ConfigProperties,
    private val agentRunner: AgentRunner,
    private val agentService: AgentService,
) {
    @Autowired
    @Qualifier("webClientBackend")
    private lateinit var webClientBackend: WebClient

    /**
     * Function that builds a base image with test resources
     *
     * @param execution [Execution] from which this workflow is started
     * @return image ID and execution command for the agent
     * @throws DockerException if interaction with docker daemon is not successful
     */
    @Suppress("UnsafeCallOnNullableType")
    fun prepareConfiguration(execution: Execution): RunConfiguration {
        val buildResult = prepareConfigurationForExecution(execution)
        log.info("For execution.id=${execution.id} using base image [${buildResult.imageTag}]")
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
    ) = agentRunner.create(
        executionId = executionId,
        configuration = configuration,
        replicas = configProperties.agentsCount,
    )

    /**
     * @param execution an [Execution] for which containers are being started
     * @param agentIds list of IDs of agents (==containers) for this execution
     * @return Flux of ticks which correspond to attempts to check agents start, completes when agents are either
     * started or timeout is reached.
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    fun startContainersAndUpdateExecution(execution: Execution, agentIds: List<String>): Flux<Long> {
        val executionId = requireNotNull(execution.id) { "For project=${execution.project} method has been called with execution with id=null" }
        log.info("Sending request to make execution.id=$executionId RUNNING")
        return webClientBackend
            .post()
            .uri("/updateExecutionByDto")
            .body(BodyInserters.fromValue(ExecutionUpdateDto(executionId, ExecutionStatus.RUNNING)))
            .retrieve()
            .toBodilessEntity()
            .map {
                agentRunner.start(execution.id!!)
                log.info("Made request to start containers for execution.id=$executionId")
            }
            .flatMapMany {
                // Check, whether the agents were actually started, if yes, all cases will be covered by themselves and HeartBeatInspector,
                // if no, mark execution as failed with internal error here
                val now = Clock.System.now()
                val duration = AtomicLong(0)
                Flux.interval(configProperties.agentsStartCheckIntervalMillis.milliseconds.toJavaDuration())
                    .takeWhile {
                        duration.get() < configProperties.agentsStartTimeoutMillis && !areAgentsHaveStarted.get()
                    }
                    .doOnNext {
                        duration.set((Clock.System.now() - now).inWholeMilliseconds)
                    }
                    .doOnComplete {
                        if (!areAgentsHaveStarted.get()) {
                            log.error("Internal error: none of agents $agentIds are started, will mark execution $executionId as failed.")
                            agentRunner.stop(executionId)
                            agentService.updateExecution(executionId, ExecutionStatus.ERROR,
                                "Internal error, raise an issue at https://github.com/saveourtool/save-cloud/issues/new"
                            ).then(agentService.markTestExecutionsAsFailed(agentIds, AgentState.CRASHED))
                                .subscribe()
                        }
                    }
            }
    }

    /**
     * @param agentIds list of IDs of agents to stop
     * @return true if agents have been stopped, false if another thread is already stopping them
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "FUNCTION_BOOLEAN_PREFIX")
    fun stopAgents(agentIds: Collection<String>) =
            try {
                agentIds.all { agentId ->
                    agentRunner.stopByAgentId(agentId)
                }
            } catch (e: AgentRunnerException) {
                log.error("Error while stopping agents $agentIds", e)
                false
            }

    /**
     * Check whether the agent agentId is stopped
     *
     * @param agentId id of an agent
     * @return true if agent is stopped
     */
    fun isAgentStopped(agentId: String): Boolean = agentRunner.isAgentStopped(agentId)

    /**
     * @param executionId ID of execution
     */
    fun cleanup(executionId: Long) {
        agentRunner.cleanup(executionId)
    }

    private fun prepareConfigurationForExecution(execution: Execution): RunConfiguration {
        val saveCliExtraArgs = SaveCliExtraArgs(
            overrideExecCmd = execution.execCmd,
            overrideExecFlags = null,
            batchSize = execution.batchSizeForAnalyzer?.takeIf { it.isNotBlank() }?.toInt(),
            batchSeparator = null,
        )
        val env = fillAgentPropertiesFromConfiguration(
            configProperties.agentSettings,
            saveCliExtraArgs,
            executionId = execution.requiredId(),
            additionalFilesString = execution.additionalFiles,
        )

        val sdk = execution.sdk.toSdk()
        val baseImage = baseImageName(sdk)
        return RunConfiguration(
            imageTag = baseImage,
            runCmd = listOf(
                "sh", "-c",
                "set -o xtrace" +
                        " && curl -vvv -X POST \$${AgentEnvName.GET_AGENT_LINK.name} --output $SAVE_AGENT_EXECUTABLE_NAME" +
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

    /**
     * @property overrideExecCmd
     * @property overrideExecFlags
     * @property batchSize
     * @property batchSeparator
     */
    internal data class SaveCliExtraArgs(
        val overrideExecCmd: String?,
        val overrideExecFlags: String?,
        val batchSize: Int?,
        val batchSeparator: String?,
    )

    companion object {
        private val log = LoggerFactory.getLogger(DockerService::class.java)
        internal const val SAVE_AGENT_EXECUTABLE_NAME = "save-agent.kexe"
    }
}

/**
 * @param sdk
 * @return name like `save-base:openjdk-11`
 */
internal fun baseImageName(sdk: Sdk) = "ghcr.io/saveourtool/save-base:${sdk.toString().replace(":", "-")}"
