package org.cqfn.save.orchestrator.service

import org.cqfn.save.domain.Python
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionUpdateDto
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.docker.ContainerManager
import org.cqfn.save.testsuite.TestSuiteDto

import com.github.dockerjava.api.exception.DockerException
import generated.SAVE_CORE_VERSION
import org.apache.commons.io.FileUtils
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
import java.nio.file.attribute.PosixFileAttributeView
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.random.Random

/**
 * A service that uses [ContainerManager] to build and start containers for test execution.
 */
@Service
@OptIn(ExperimentalPathApi::class)
class DockerService(private val configProperties: ConfigProperties) {
    /**
     * [ContainerManager] that is used to access docker daemon API
     */
    internal val containerManager = ContainerManager(configProperties.docker)
    private val executionDir = "/run/save-execution"
    private val standardTestSuiteDir = "standard-test-suites"

    @Suppress("NonBooleanPropertyPrefixedWithIs")
    private val isAgentStoppingInProgress = AtomicBoolean(false)

    @Autowired
    @Qualifier("webClientBackend")
    private lateinit var webClientBackend: WebClient

    /**
     * Function that builds a base image with test resources and then creates containers with agents.
     *
     * @param execution [Execution] from which this workflow is started
     * @param testSuiteDtos test suites, selected by user
     * @return list of IDs of created containers
     * @throws DockerException if interaction with docker daemon is not successful
     */
    fun buildAndCreateContainers(execution: Execution, testSuiteDtos: List<TestSuiteDto>?): List<String> {
        log.info("Building base image for execution.id=${execution.id}")
        val (imageId, runCmd, saveCliExecFlags) = buildBaseImageForExecution(execution, testSuiteDtos)
        log.info("Built base image for execution.id=${execution.id}")
        return (1..configProperties.agentsCount).map { number ->
            log.info("Building container #$number for execution.id=${execution.id}")
            createContainerForExecution(execution, imageId, "${execution.id}-$number", runCmd, saveCliExecFlags).also {
                log.info("Built container id=$it for execution.id=${execution.id}")
            }
        }
    }

    /**
     * @param execution an [Execution] for which containers are being started
     * @param agentIds list of IDs of agents (==containers) for this execution
     */
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
        agentIds.forEach {
            log.info("Starting container id=$it")
            containerManager.dockerClient.startContainerCmd(it).exec()
        }
        log.info("Successfully started all containers for execution.id=$executionId")
    }

    /**
     * @param agentIds list of IDs of agents to stop
     * @return true if agents have been stopped, false if another thread is already stopping them
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun stopAgents(agentIds: List<String>) =
            if (isAgentStoppingInProgress.compareAndSet(false, true)) {
                try {
                    val containerList = containerManager.dockerClient.listContainersCmd().withShowAll(true).exec()
                    val runningContainersIds = containerList.filter { it.state == "running" }.map { it.id }
                    agentIds.forEach { agentId ->
                        if (agentId in runningContainersIds) {
                            log.info("Stopping agent with id=$agentId")
                            containerManager.dockerClient.stopContainerCmd(agentId).exec()
                            log.info("Agent with id=$agentId has been stopped")
                        } else {
                            val state = containerList.find { it.id == agentId }?.state ?: "deleted"
                            val warnMsg = "Agent with id=$agentId was requested to be stopped, but it actual state=$state"
                            log.warn(warnMsg)
                        }
                    }
                    true
                } catch (dex: DockerException) {
                    log.error("Error while stopping agents $agentIds", dex)
                    false
                } finally {
                    isAgentStoppingInProgress.lazySet(false)
                }
            } else {
                log.info("Agents stopping is already in progress, skipping")
                false
            }

    /**
     * @param imageName name of the image to remove
     * @return an instance of docker command
     */
    fun removeImage(imageName: String) {
        log.info("Removing image $imageName")
        val existingImages = containerManager.dockerClient.listImagesCmd().exec().map {
            it.id
        }
        if (imageName in existingImages) {
            containerManager.dockerClient.removeImageCmd(imageName).exec()
        } else {
            log.info("Image $imageName is not present, so won't attempt to remove")
        }
    }

    /**
     * @param containerId id of container to remove
     * @return an instance of docker command
     */
    fun removeContainer(containerId: String) {
        log.info("Removing container $containerId")
        val existingContainerIds = containerManager.dockerClient.listContainersCmd().withShowAll(true).exec()
            .map {
                it.id
            }
        if (containerId in existingContainerIds) {
            containerManager.dockerClient.removeContainerCmd(containerId).exec()
        } else {
            log.info("Container $containerId is not present, so won't attempt to remove")
        }
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "UnsafeCallOnNullableType",
        "LongMethod",
    )
    private fun buildBaseImageForExecution(execution: Execution, testSuiteDtos: List<TestSuiteDto>?): Triple<String, String, String> {
        val resourcesPath = File(
            configProperties.testResources.basePath,
            execution.resourcesRootPath,
        )
        val agentRunCmd = "./$SAVE_AGENT_EXECUTABLE_NAME"

        // collect standard test suites for docker image, which were selected by user, if any
        val testSuitesForDocker = collectStandardTestSuitesForDocker(testSuiteDtos)
        val testSuitesDir = resourcesPath.resolve(standardTestSuiteDir)
        // copy corresponding standard test suites to resourcesRootPath dir
        copyTestSuitesToResourcesPath(testSuitesForDocker, testSuitesDir)
        val saveCliExecFlags = if (testSuitesForDocker.isNotEmpty()) {
            // create stub toml config in aim to execute all test suites directories from `testSuitesDir`
            testSuitesDir.resolve("save.toml").apply { createNewFile() }.writeText("[general]")
            " \"$standardTestSuiteDir\" --include-suites \"${testSuitesForDocker.joinToString(" ") { it.name }}\""
        } else {
            ""
        }

        // include save-agent into the image
        val saveAgent = File(resourcesPath, SAVE_AGENT_EXECUTABLE_NAME)
        FileUtils.copyInputStreamToFile(
            ClassPathResource(SAVE_AGENT_EXECUTABLE_NAME).inputStream,
            saveAgent
        )
        // include save-cli into the image
        val saveCli = File(resourcesPath, SAVE_CLI_EXECUTABLE_NAME)
        FileUtils.copyInputStreamToFile(
            ClassPathResource(SAVE_CLI_EXECUTABLE_NAME).inputStream,
            saveCli
        )
        val baseImage = execution.sdk
        val aptCmd = "apt-get ${configProperties.aptExtraFlags}"
        // fixme: https://github.com/diktat-static-analysis/save-cloud/issues/352
        val additionalRunCmd = if (execution.sdk.startsWith(Python.NAME, ignoreCase = true)) {
            """|RUN env DEBIAN_FRONTEND="noninteractive" $aptCmd install zip
               |RUN curl -s "https://get.sdkman.io" | bash
               |RUN bash -c 'source "${'$'}HOME/.sdkman/bin/sdkman-init.sh" && sdk install java 8.0.302-open'
               |RUN ln -s ${'$'}(which java) /usr/bin/java
            """.trimMargin()
        } else {
            ""
        }
        val imageId = containerManager.buildImageWithResources(
            baseImage = baseImage,
            imageName = imageName(execution.id!!),
            baseDir = resourcesPath,
            resourcesPath = executionDir,
            runCmd = """RUN $aptCmd update && env DEBIAN_FRONTEND="noninteractive" $aptCmd install -y \
                    |libcurl4-openssl-dev tzdata
                    |RUN ln -fs /usr/share/zoneinfo/UTC /etc/localtime
                    |$additionalRunCmd
                    |RUN rm -rf /var/lib/apt/lists/*
                    |RUN chmod +x $executionDir/$SAVE_AGENT_EXECUTABLE_NAME
                    |RUN chmod +x $executionDir/$SAVE_CLI_EXECUTABLE_NAME
                """
        )
        saveAgent.delete()
        saveCli.delete()
        return Triple(imageId, agentRunCmd, saveCliExecFlags)
    }

    private fun collectStandardTestSuitesForDocker(testSuiteDtos: List<TestSuiteDto>?): List<TestSuiteDto> {
        val testSuitesForDocker = testSuiteDtos?.flatMap {
            webClientBackend.get()
                .uri("/standardTestSuitesWithName?name=${it.name}")
                .retrieve()
                .bodyToMono<List<TestSuite>>()
                .block()!!
        }?.map { it.toDto() } ?: emptyList()
        return testSuitesForDocker
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun copyTestSuitesToResourcesPath(testSuitesForDocker: List<TestSuiteDto>, destination: File) {
        // TODO: https://github.com/diktat-static-analysis/save-cloud/issues/321
        log.info("Copying suites ${testSuitesForDocker.map { it.name }} into $destination")
        testSuitesForDocker.forEach {
            val standardTestSuiteAbsolutePath = File(configProperties.testResources.basePath)
                // tmp directories names for standard test suites constructs just by hashCode of listOf(repoUrl); reuse this logic
                .resolve(File("${listOf(it.testSuiteRepoUrl!!).hashCode()}")
                    .resolve(it.testRootPath)
                )
            log.debug("Copying suite ${it.name} from $standardTestSuiteAbsolutePath into $destination/...")
            standardTestSuiteAbsolutePath.copyRecursively(destination.resolve("${it.name}_${it.testRootPath.hashCode()}_${Random.nextInt()}"))
        }
        // orchestrator is executed as root (to access docker socket), but files are in a shared volume
        val lookupService = destination.toPath().fileSystem.userPrincipalLookupService
        destination.walk().forEach {
            Files.getFileAttributeView(it.toPath(), PosixFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS).apply {
                setGroup(lookupService.lookupPrincipalByGroupName("cnb"))
                setOwner(lookupService.lookupPrincipalByName("cnb"))
            }
        }
    }

    private fun createContainerForExecution(
        execution: Execution,
        imageId: String,
        containerNumber: String,
        runCmd: String,
        saveCliExecFlags: String,
    ): String {
        val containerId = containerManager.createContainerFromImage(
            imageId,
            executionDir,
            runCmd,
            containerName(containerNumber),
        )
        val agentPropertiesFile = createTempDirectory("agent")
            .resolve("agent.properties")
            .toFile()
        FileUtils.copyInputStreamToFile(
            ClassPathResource("agent.properties").inputStream,
            agentPropertiesFile
        )
        val cliCommand = "./$SAVE_CLI_EXECUTABLE_NAME$saveCliExecFlags"
        agentPropertiesFile.writeText(
            agentPropertiesFile.readLines().joinToString(System.lineSeparator()) {
                if (it.startsWith("id=")) {
                    "id=$containerId"
                } else if (it.startsWith("cliCommand=")) {
                    "cliCommand=$cliCommand"
                } else {
                    it
                }
            }
        )
        containerManager.copyResourcesIntoContainer(
            containerId, executionDir,
            listOf(agentPropertiesFile)
        )
        return containerId
    }

    companion object {
        private val log = LoggerFactory.getLogger(DockerService::class.java)
        private const val SAVE_AGENT_EXECUTABLE_NAME = "save-agent.kexe"
        private const val SAVE_CLI_EXECUTABLE_NAME = "save-$SAVE_CORE_VERSION-linuxX64.kexe"
    }
}

/**
 * @param executionId
 */
internal fun imageName(executionId: Long) = "save-execution:$executionId"

/**
 * @param id
 */
internal fun containerName(id: String) = "save-execution-$id"
