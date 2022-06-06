package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.domain.Python
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.orchestrator.SAVE_CLI_EXECUTABLE_NAME
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.copyRecursivelyWithAttributes
import com.saveourtool.save.orchestrator.createSyntheticTomlConfig
import com.saveourtool.save.orchestrator.docker.AgentRunner
import com.saveourtool.save.orchestrator.docker.DockerContainerManager
import com.saveourtool.save.orchestrator.fillAgentPropertiesFromConfiguration
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE
import com.saveourtool.save.utils.STANDARD_TEST_SUITE_DIR
import com.saveourtool.save.utils.moveFileWithAttributes

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.DockerException
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributeView
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.io.path.ExperimentalPathApi

/**
 * A service that uses [DockerContainerManager] to build and start containers for test execution.
 * @property dockerContainerManager [DockerContainerManager] that is used to access docker daemon API
 */
@Service
@OptIn(ExperimentalPathApi::class)
class DockerService(private val configProperties: ConfigProperties,
                    private val dockerClient: DockerClient,
                    internal val dockerContainerManager: DockerContainerManager,
                    private val agentRunner: AgentRunner,
) {
    private val executionDir = "/run/save-execution"

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
    @Suppress("UnsafeCallOnNullableType")
    fun buildAndCreateContainers(
        execution: Execution,
        testSuiteDtos: List<TestSuiteDto>?,
    ): List<String> {
        log.info("Building base image for execution.id=${execution.id}")
        val (imageId, agentRunCmd) = buildBaseImageForExecution(execution, testSuiteDtos)
        // todo (k8s): need to also push it so that other nodes will have access to it
        log.info("Built base image for execution.id=${execution.id}")

        return agentRunner.create(
            executionId = execution.id!!,
            baseImageId = imageId,
            replicas = configProperties.agentsCount,
            agentRunCmd = agentRunCmd,
            workingDir = executionDir,
        )
    }

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
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun stopAgents(agentIds: Collection<String>) =
            if (isAgentStoppingInProgress.compareAndSet(false, true)) {
                try {
                    agentIds.forEach { agentId ->
                        agentRunner.stopByAgentId(agentId)
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
    private fun buildBaseImageForExecution(
        execution: Execution,
        testSuiteDtos: List<TestSuiteDto>?
    ): Pair<String, String> {
        val resourcesPath = File(
            configProperties.testResources.basePath,
            execution.resourcesRootPath!!,
        )
        val agentRunCmd = "./$SAVE_AGENT_EXECUTABLE_NAME"

        // collect standard test suites for docker image, which were selected by user, if any
        val testSuitesForDocker = collectStandardTestSuitesForDocker(testSuiteDtos)
        val testSuitesDir = resourcesPath.resolve(STANDARD_TEST_SUITE_DIR)

        // list is not empty only in standard mode
        val isStandardMode = testSuitesForDocker.isNotEmpty()

        if (isStandardMode) {
            // copy corresponding standard test suites to resourcesRootPath dir
            copyTestSuitesToResourcesPath(testSuitesForDocker, testSuitesDir)
            // move additional files, which were downloaded into the root dit to the execution dir for standard suites
            execution.additionalFiles?.split(";")?.filter { it.isNotBlank() }?.forEach {
                val additionalFilePath = resourcesPath.resolve(File(it).name)
                log.info("Move additional file $additionalFilePath into $testSuitesDir")
                moveFileWithAttributes(additionalFilePath, testSuitesDir)
            }
        }

        // if some additional file is archive, unzip it into proper destination:
        // for standard mode into STANDARD_TEST_SUITE_DIR
        // for Git mode into testRootPath
        unzipArchivesAmongAdditionalFiles(execution, isStandardMode, testSuitesDir, resourcesPath)

        val saveCliExecFlags = if (isStandardMode) {
            // create stub toml config in aim to execute all test suites directories from `testSuitesDir`
            val configData = createSyntheticTomlConfig(execution.execCmd, execution.batchSizeForAnalyzer)

            testSuitesDir.resolve("save.toml").apply { createNewFile() }.writeText(configData)
            " $STANDARD_TEST_SUITE_DIR --include-suites \"${testSuitesForDocker.joinToString(",") { it.name }}\""
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

        if (configProperties.adjustResourceOwner) {
            changeOwnerRecursively(resourcesPath, "cnb")
        }

        val agentPropertiesFile = File(resourcesPath, "agent.properties")
        fillAgentPropertiesFromConfiguration(agentPropertiesFile, configProperties.agentSettings, saveCliExecFlags)

        val sdk = execution.sdk.toSdk()
        val baseImage = baseImageName(sdk)
        dockerContainerManager.findImages(baseImage).ifEmpty {
            log.info("Base image [$baseImage] for execution ${execution.id} doesn't exists, will build it first")
            buildBaseImage(sdk)
        }
        val aptCmd = "apt-get ${configProperties.aptExtraFlags}"
        val imageId = dockerContainerManager.buildImageWithResources(
            baseImage = baseImage,
            imageName = imageName(execution.id!!),
            baseDir = resourcesPath,
            resourcesTargetPath = executionDir,
            runCmd = """RUN $aptCmd update && env DEBIAN_FRONTEND="noninteractive" $aptCmd install -y \
                    |libcurl4-openssl-dev tzdata
                    |RUN ln -fs /usr/share/zoneinfo/UTC /etc/localtime
                    |RUN rm -rf /var/lib/apt/lists/*
                    |RUN chmod +x $executionDir/$SAVE_AGENT_EXECUTABLE_NAME
                    |RUN chmod +x $executionDir/$SAVE_CLI_EXECUTABLE_NAME
                """
        )
        saveAgent.delete()
        saveCli.delete()
        return Pair(imageId, agentRunCmd)
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
            """|RUN $aptCmd update
               |RUN env DEBIAN_FRONTEND="noninteractive" $aptCmd install zip
               |RUN curl -s "https://get.sdkman.io" | bash
               |RUN bash -c 'source "${'$'}HOME/.sdkman/bin/sdkman-init.sh" && sdk install java 8.0.302-open'
               |RUN ln -s ${'$'}(which java) /usr/bin/java
            """.trimMargin()
        } else {
            ""
        }

        return dockerContainerManager.buildImageWithResources(
            baseImage = sdk.toString(),
            imageName = baseImageName(sdk),
            baseDir = null,
            resourcesTargetPath = null,
            runCmd = additionalRunCmd
        ).also {
            log.debug("Successfully built base image id=$it")
        }
    }

    private fun changeOwnerRecursively(directory: File, user: String) {
        // orchestrator is executed as root (to access docker socket), but files are in a shared volume
        val lookupService = directory.toPath().fileSystem.userPrincipalLookupService
        directory.walk().forEach { file ->
            Files.getFileAttributeView(file.toPath(), PosixFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS).apply {
                setGroup(lookupService.lookupPrincipalByGroupName(user))
                setOwner(lookupService.lookupPrincipalByName(user))
            }
        }
    }

    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    private fun unzipArchivesAmongAdditionalFiles(
        execution: Execution,
        isStandardMode: Boolean,
        testSuitesDir: File,
        resourcesPath: File,
    ) {
        // FixMe: for now support only .zip files
        execution.additionalFiles?.split(";")?.filter { it.endsWith(".zip") }?.forEach { fileName ->
            val fileLocation = if (isStandardMode) {
                testSuitesDir
            } else {
                val testRootPath = webClientBackend.post()
                    .uri("/findTestRootPathForExecutionByTestSuites")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(execution))
                    .retrieve()
                    .bodyToMono<List<String>>()
                    .block()!!
                    .distinct()
                    .single()
                resourcesPath.resolve(testRootPath)
            }

            val file = fileLocation.resolve(File(fileName).name)
            val shouldBeExecutable = file.canExecute()
            log.debug("Unzip ${file.absolutePath} into ${fileLocation.absolutePath}")

            file.unzipInto(fileLocation)
            if (shouldBeExecutable) {
                log.info("Marking files in $fileLocation executable...")
                fileLocation.walkTopDown().forEach { source ->
                    if (!source.setExecutable(true)) {
                        log.warn("Failed to mark file ${source.name} as executable")
                    }
                }
            }
            file.delete()
        }
    }

    private fun File.unzipInto(destination: File) {
        try {
            val zipFile = ZipFile(this.toString())
            zipFile.extractAll(destination.toString())
        } catch (e: ZipException) {
            log.error("Error occurred during extracting of archive ${this.name}")
            e.printStackTrace()
        }
    }

    private fun collectStandardTestSuitesForDocker(testSuiteDtos: List<TestSuiteDto>?): List<TestSuiteDto> = testSuiteDtos?.flatMap {
        webClientBackend.get()
            .uri("/standardTestSuitesWithName?name=${it.name}")
            .retrieve()
            .bodyToMono<List<TestSuite>>()
            .block()!!
    }?.map { it.toDto() } ?: emptyList()

    @Suppress("UnsafeCallOnNullableType", "TOO_MANY_LINES_IN_LAMBDA")
    private fun copyTestSuitesToResourcesPath(testSuitesForDocker: List<TestSuiteDto>, destination: File) {
        FileSystemUtils.deleteRecursively(destination)
        // TODO: https://github.com/saveourtool/save-cloud/issues/321
        log.info("Copying suites ${testSuitesForDocker.map { it.name }} into $destination")
        testSuitesForDocker.forEach {
            val standardTestSuiteAbsolutePath = File(configProperties.testResources.basePath)
                // tmp directories names for standard test suites constructs just by hashCode of listOf(repoUrl); reuse this logic
                .resolve(File("${listOf(it.testSuiteRepoUrl!!).hashCode()}")
                    .resolve(it.testRootPath)
                )
            val currentSuiteDestination = destination.resolve(getLocationInStandardDirForTestSuite(it))
            if (!currentSuiteDestination.exists()) {
                log.debug("Copying suite ${it.name} from $standardTestSuiteAbsolutePath into $currentSuiteDestination/...")
                copyRecursivelyWithAttributes(standardTestSuiteAbsolutePath, currentSuiteDestination)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DockerService::class.java)
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
 * @param testSuiteDto
 */
internal fun getLocationInStandardDirForTestSuite(testSuiteDto: TestSuiteDto) =
        "$PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE${testSuiteDto.testSuiteRepoUrl.hashCode()}_${testSuiteDto.testRootPath.hashCode()}"
