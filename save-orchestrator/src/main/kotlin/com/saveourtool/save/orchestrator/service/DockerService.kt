package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.orchestrator.SAVE_CLI_EXECUTABLE_NAME
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.fillAgentPropertiesFromConfiguration
import com.saveourtool.save.orchestrator.runner.AgentRunner
import com.saveourtool.save.orchestrator.runner.AgentRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.runner.TEST_SUITES_DIR_NAME
import com.saveourtool.save.orchestrator.utils.LoggingContextImpl
import com.saveourtool.save.orchestrator.utils.changeOwnerRecursively
import com.saveourtool.save.orchestrator.utils.tryMarkAsExecutable
import com.saveourtool.save.utils.DATABASE_DELIMITER
import com.saveourtool.save.utils.orConflict

import org.apache.commons.io.file.PathUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

import kotlin.io.path.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlinx.datetime.Clock
import java.nio.file.Paths

/**
 * A service that uses [DockerContainerManager] to build and start containers for test execution.
 */
@Service
@OptIn(ExperimentalPathApi::class)
class DockerService(
    private val configProperties: ConfigProperties,
    private val agentRunner: AgentRunner,
    private val persistentVolumeService: PersistentVolumeService,
    private val agentService: AgentService,
) {
    // Somehow simple path.createDirectories() doesn't work on macOS, probably due to Apple File System features
    private val tmpDir = Paths.get(configProperties.testResources.tmpPath).let {
        if (it.exists()) {
            it
        } else {
            it.createDirectories()
        }
    }

    @Suppress("NonBooleanPropertyPrefixedWithIs")
    private val isAgentStoppingInProgress = AtomicBoolean(false)

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
    fun prepareConfiguration(execution: Execution): RunConfiguration<PersistentVolumeId> {
        val resourcesForExecution = createTempDirectory(
            directory = tmpDir,
            prefix = "save-execution-${execution.id}"
        )
        log.info("Preparing volume for execution.id=${execution.id}")
        val buildResult = prepareImageAndVolumeForExecution(resourcesForExecution, execution)
        // todo (k8s): need to also push it so that other nodes will have access to it
        log.info("For execution.id=${execution.id} using base image [${buildResult.imageTag}] and PV [id=${buildResult.pvId}]")
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
        configuration: RunConfiguration<PersistentVolumeId>,
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
                            log.error("Internal error: none of agents $agentIds are started, will mark execution as failed.")
                            agentRunner.stop(executionId)
                            agentService.updateExecution(executionId, ExecutionStatus.ERROR,
                                "Internal error, raise an issue at https://github.com/saveourtoo/save-cloud/issues/new"
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
            if (isAgentStoppingInProgress.compareAndSet(false, true)) {
                try {
                    agentIds.all { agentId ->
                        agentRunner.stopByAgentId(agentId)
                    }
                } catch (e: AgentRunnerException) {
                    log.error("Error while stopping agents $agentIds", e)
                    false
                } finally {
                    isAgentStoppingInProgress.lazySet(false)
                }
            } else {
                log.info("Agents stopping is already in progress, skipping")
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
     * @param executionId
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun stop(executionId: Long): Boolean {
        // return if (isAgentStoppingInProgress.compute(executionId) { _, value -> if (value == false) true else value } == true) {
        return if (isAgentStoppingInProgress.compareAndSet(false, true)) {
            try {
                agentRunner.stop(executionId)
                true
            } finally {
                isAgentStoppingInProgress.lazySet(false)
            }
        } else {
            false
        }
    }

    /**
     * @param executionId ID of execution
     */
    fun cleanup(executionId: Long) {
        agentRunner.cleanup(executionId)
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "UnsafeCallOnNullableType",
        "LongMethod",
    )
    private fun prepareImageAndVolumeForExecution(resourcesForExecution: Path, execution: Execution): RunConfiguration<PersistentVolumeId> {
        // collect test suite names, which were selected by user
        val saveCliExecFlags = " --include-suites \"${execution.getTestSuiteNames().joinToString(DATABASE_DELIMITER)}\" $TEST_SUITES_DIR_NAME"

        // include save-agent into the image
        PathUtils.copyFile(
            ClassPathResource(SAVE_AGENT_EXECUTABLE_NAME).url,
            resourcesForExecution.resolve(SAVE_AGENT_EXECUTABLE_NAME)
        )

        // include save-cli into the image
        PathUtils.copyFile(
            ClassPathResource(SAVE_CLI_EXECUTABLE_NAME).url,
            resourcesForExecution.resolve(SAVE_CLI_EXECUTABLE_NAME)
        )

        if (configProperties.adjustResourceOwner) {
            // orchestrator is executed as root (to access docker socket), but files are in a shared volume
            // todo: set it to `save-agent` (by ID returned from Docker build?)
            resourcesForExecution.changeOwnerRecursively("cnb")

            with(loggingContext) {
                resourcesForExecution.resolve(SAVE_AGENT_EXECUTABLE_NAME).tryMarkAsExecutable()
                resourcesForExecution.resolve(SAVE_CLI_EXECUTABLE_NAME).tryMarkAsExecutable()
            }
        }

        val agentPropertiesFile = resourcesForExecution.resolve("agent.properties")
        fillAgentPropertiesFromConfiguration(agentPropertiesFile.toFile(), configProperties.agentSettings, saveCliExecFlags)

        val pvId = persistentVolumeService.createFromResources(resourcesForExecution)
        log.info("Built persistent volume with tests and additional files by id $pvId")
        // FixMe: temporary moved after `AgentRunner.start`
        // FileSystemUtils.deleteRecursively(resourcesForExecution)

        val sdk = execution.sdk.toSdk()
        val baseImage = baseImageName(sdk)
        return RunConfiguration(
            imageTag = baseImage,
            runCmd = listOf("sh", "-c", "chmod +x $SAVE_AGENT_EXECUTABLE_NAME && ./$SAVE_AGENT_EXECUTABLE_NAME"),
            pvId = pvId,
            resourcesPath = resourcesForExecution,
            resourcesConfiguration = RunConfiguration.ResourcesConfiguration(
                executionId = execution.requiredId(),
                additionalFilesString = execution.additionalFiles,
            ),
        )
    }

    private fun Execution.getTestSuiteNames(): List<String> = this
        .parseAndGetTestSuiteIds()
        ?.let {
            webClientBackend.post()
                .uri("/test-suite/names-by-ids")
                .bodyValue(it)
                .retrieve()
                .bodyToMono<List<String>>()
                .block()!!
        }.orConflict {
            "Execution (id=$id) doesn't contain testSuiteIds"
        }

    /**
     * Information required to start containers with save-agent
     *
     * @property imageTag tag of an image which should be used for a container
     * @property runCmd command that should be run as container's entrypoint.
     * Usually looks like `sh -c "rest of the command"`.
     * @property pvId ID of a persistent volume that should be attached to a container
     * @property resourcesPath FixMe: needed only until agents download test and additional files by themselves
     * @property workingDir
     * @property resourcesConfiguration
     */
    data class RunConfiguration<I : PersistentVolumeId>(
        val imageTag: String,
        val runCmd: List<String>,
        val pvId: I,
        val workingDir: String = EXECUTION_DIR,
        val resourcesPath: Path,
        val resourcesConfiguration: ResourcesConfiguration,
    ) {
        /**
         * @property executionId
         * @property additionalFilesString
         */
        data class ResourcesConfiguration(
            val executionId: Long,
            val additionalFilesString: String,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(DockerService::class.java)
        private val loggingContext = LoggingContextImpl(log)
        private const val SAVE_AGENT_EXECUTABLE_NAME = "save-agent.kexe"
    }
}

/**
 * @param sdk
 * @return name like `save-base:openjdk-11`
 */
internal fun baseImageName(sdk: Sdk) = "ghcr.io/saveourtool/save-base:${sdk.toString().replace(":", "-")}"
