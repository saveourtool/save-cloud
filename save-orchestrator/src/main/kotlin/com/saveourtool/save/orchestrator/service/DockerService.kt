package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.domain.Python
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.orchestrator.SAVE_CLI_EXECUTABLE_NAME
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.createSyntheticTomlConfig
import com.saveourtool.save.orchestrator.docker.DockerContainerManager
import com.saveourtool.save.orchestrator.fillAgentPropertiesFromConfiguration
import com.saveourtool.save.orchestrator.runner.AgentRunner
import com.saveourtool.save.orchestrator.runner.AgentRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.utils.LoggingContextImpl
import com.saveourtool.save.orchestrator.utils.changeOwnerRecursively
import com.saveourtool.save.orchestrator.utils.tryMarkAsExecutable
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE
import com.saveourtool.save.utils.STANDARD_TEST_SUITE_DIR

import com.github.dockerjava.api.DockerClient
import com.saveourtool.save.orchestrator.runner.TEST_SUITES_DIR_NAME
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

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

/**
 * A service that uses [DockerContainerManager] to build and start containers for test execution.
 * @property dockerContainerManager [DockerContainerManager] that is used to access docker daemon API
 */
@Service
@OptIn(ExperimentalPathApi::class)
class DockerService(
    private val configProperties: ConfigProperties,
    private val dockerClient: DockerClient,
    internal val dockerContainerManager: DockerContainerManager,
    private val agentRunner: AgentRunner,
    private val persistentVolumeService: PersistentVolumeService,
) {
    private val testSuiteDirName = "test-suites"

    @Suppress("NonBooleanPropertyPrefixedWithIs")
    private val isAgentStoppingInProgress = AtomicBoolean(false)

    @Autowired
    @Qualifier("webClientBackend")
    private lateinit var webClientBackend: WebClient

    /**
     * Function that builds a base image with test resources
     *
     * @param dockerImageLocation location to base image
     * @param execution [Execution] from which this workflow is started
     * @return image ID and execution command for the agent
     * @throws DockerException if interaction with docker daemon is not successful
     */
    @Suppress("UnsafeCallOnNullableType")
    fun prepareConfiguration(dockerImageLocation: Path, execution: Execution): RunConfiguration<PersistentVolumeId> {
        log.info("Preparing image and volume for execution.id=${execution.id}")
        val buildResult = prepareImageAndVolumeForExecution(dockerImageLocation, execution)
        // todo (k8s): need to also push it so that other nodes will have access to it
        log.info("For execution.id=${execution.id} using base image [id=${buildResult.imageId}] and PV [id=${buildResult.pvId}]")
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
        workingDir = EXECUTION_DIR,
    )

    /**
     * @param execution an [Execution] for which containers are being started
     * @param agentIds list of IDs of agents (==containers) for this execution
     */
    @Suppress("UnsafeCallOnNullableType")
    fun startContainersAndUpdateExecution(execution: Execution, agentIds: List<String>) {
        val executionId = requireNotNull(execution.id) { "For project=${execution.project} method has been called with execution with id=null" }
        log.info("Sending request to make execution.id=$executionId RUNNING")
        webClientBackend
            .post()
            .uri("/updateExecutionByDto")
            .body(BodyInserters.fromValue(ExecutionUpdateDto(executionId, ExecutionStatus.RUNNING)))
            .retrieve()
            .toBodilessEntity()
            .subscribe()
        agentRunner.start(execution.id!!)
        log.info("Successfully started all containers for execution.id=$executionId")
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
     * @param imageName name of the image to remove
     * @return an instance of docker command
     */
    fun removeImage(imageName: String) {
        log.info("Removing image $imageName")
        val existingImages = dockerClient.listImagesCmd().exec().map {
            it.id
        }
        if (imageName in existingImages) {
            dockerClient.removeImageCmd(imageName).exec()
        } else {
            log.info("Image $imageName is not present, so won't attempt to remove")
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
        // create stub toml config in aim to execute all test suites directories from `testSuitesDir`
        resourcesForExecution.resolve(TEST_SUITES_DIR_NAME)
            .resolve("save.toml")
            .apply { createFile() }
            .writeText(createSyntheticTomlConfig(execution.execCmd, execution.batchSizeForAnalyzer))
        // collect test suite names, which were selected by user
        val saveCliExecFlags = " $TEST_SUITES_DIR_NAME --include-suites \"${execution.getTestSuiteNames()}\""

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

        val pvId = persistentVolumeService.createFromResources(listOf(resourcesForExecution))
        log.info("Built persistent volume with tests by id $pvId")

        val sdk = execution.sdk.toSdk()
        val baseImage = baseImageName(sdk)
        val baseImageId: String = dockerContainerManager.findImages(saveId = baseImage)
            .map { it.id }
            .ifEmpty {
                log.info("Base image [$baseImage] for execution ${execution.id} doesn't exists, will build it first")
                listOf(buildBaseImage(sdk))
            }
            .first()
        return RunConfiguration(
            imageId = baseImageId,
            runCmd = "sh -c \"chmod +x $SAVE_AGENT_EXECUTABLE_NAME && ./$SAVE_AGENT_EXECUTABLE_NAME\"",
            pvId = pvId,
        )
    }

    /**
     * @param sdk
     * @return an ID of the built image or of an existing one
     */
    fun buildBaseImage(sdk: Sdk): String {
        val images = dockerContainerManager.findImages(baseImageName(sdk))
        if (images.isNotEmpty()) {
            log.info("Base image for sdk=$sdk already exists, skipping build")
            return images.first().id
        }
        log.info("Starting to build base image for sdk=$sdk")

        val aptCmd = "apt-get ${configProperties.aptExtraFlags}"
        // fixme: https://github.com/saveourtool/save-cloud/issues/352
        val additionalRunCmd = if (sdk is Python) {
            """|RUN curl -s "https://get.sdkman.io" | bash
               |RUN bash -c 'source "${'$'}HOME/.sdkman/bin/sdkman-init.sh" && sdk install java 8.0.302-open'
               |RUN ln -s ${'$'}(which java) /usr/bin/java
            """.trimMargin()
        } else {
            ""
        }

        return dockerContainerManager.buildImage(
            baseImage = sdk.toString(),
            imageName = baseImageName(sdk),
            runCmd = """|RUN $aptCmd update && env DEBIAN_FRONTEND="noninteractive" $aptCmd install -y \
                    |libcurl4-openssl-dev tzdata
                    |RUN ln -fs /usr/share/zoneinfo/UTC /etc/localtime
                    |RUN rm -rf /var/lib/apt/lists/*
                    |$additionalRunCmd
                    |RUN groupadd --gid 1100 save-agent && useradd --uid 1100 --gid 1100 --create-home --shell /bin/sh save-agent
                    |WORKDIR $EXECUTION_DIR
            """.trimMargin()
        ).also {
            log.debug("Successfully built base image id=$it")
        }
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
     * @property imageId ID of an image which should be used for a container
     * @property runCmd command that should be run as container's entrypoint
     * @property pvId ID of a persistent volume that should be attached to a container
     */
    data class RunConfiguration<I : PersistentVolumeId>(
        val imageId: String,
        val runCmd: String,
        val pvId: I,
    )

    companion object {
        private val log = LoggerFactory.getLogger(DockerService::class.java)
        private val loggingContext = LoggingContextImpl(log)
        private const val SAVE_AGENT_EXECUTABLE_NAME = "save-agent.kexe"
    }
}

/**
 * @param executionId
 */
internal fun imageName(executionId: Long) = "save-execution:$executionId"

/**
 * @param sdk
 */
internal fun baseImageName(sdk: Sdk) = "save-base-$sdk"

/**
 * @param imageName
 * @return whether [imageName] refers to a base image for save execution
 */
internal fun isBaseImageName(imageName: String) = imageName.startsWith("save-base-")
