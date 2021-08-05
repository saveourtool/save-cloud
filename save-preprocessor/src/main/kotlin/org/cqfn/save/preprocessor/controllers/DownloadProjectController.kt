package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.defaultConfig
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.execution.ExecutionInitializationDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.execution.ExecutionUpdateDto
import org.cqfn.save.preprocessor.EmptyResponse
import org.cqfn.save.preprocessor.StatusResponse
import org.cqfn.save.preprocessor.TextResponse
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.service.TestDiscoveringService
import org.cqfn.save.preprocessor.utils.decodeFromPropertiesFile
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory

import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.io.File
import java.util.stream.Collectors

import kotlin.io.path.ExperimentalPathApi

/**
 * A Spring controller for git project downloading
 *
 * @property configProperties config properties
 */
@OptIn(ExperimentalPathApi::class)
@RestController
class DownloadProjectController(private val configProperties: ConfigProperties,
                                private val testDiscoveringService: TestDiscoveringService,
) {
    private val log = LoggerFactory.getLogger(DownloadProjectController::class.java)
    private val webClientBackend = WebClient.create(configProperties.backend)
    private val webClientOrchestrator = WebClient.create(configProperties.orchestrator)


    /**
     * @param executionRequest - Dto of repo information to clone and project info
     * @return response entity with text
     */
    @PostMapping("/upload")
    fun upload(@RequestBody executionRequest: ExecutionRequest): Mono<TextResponse> = Mono.just(ResponseEntity("Clone pending", HttpStatus.ACCEPTED))
        .doOnSuccess {
            downLoadRepository(executionRequest)
                .flatMap { (location, version) ->
                    updateExecution(executionRequest.project, location, version).map { execution ->
                        Pair(execution, location)
                    }
                }
                .flatMap { (execution, location) ->
                    sendToBackendAndOrchestrator(
                        execution,
                        executionRequest.project,
                        executionRequest.propertiesRelativePath,
                        location,
                        null
                    )
                }
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }

    /**
     * @param executionRequestForStandardSuites - Dto of binary file, test suites names and project info
     * @param propertyFile
     * @param binaryFile
     * @return response entity with text
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    @PostMapping(value = ["/uploadBin"], consumes = ["multipart/form-data"])
    fun uploadBin(
        @RequestPart executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        @RequestPart("property", required = true) propertyFile: Mono<FilePart>,
        @RequestPart("binFile", required = true) binaryFile: Mono<FilePart>,
    ) = Mono.just(ResponseEntity("Clone pending", HttpStatus.ACCEPTED))
        .doOnSuccess { _ ->
            val binFile = File("program")
            val propFile = File("save.properties")
            Mono.zip(
                propertyFile.flatMapMany { it.content() }.collectList(),
                binaryFile.flatMapMany { it.content() }.collectList()
            ).flatMap { (propertyFileContent, binaryFileContent) ->
                propertyFileContent.map { dtBuffer -> propFile.outputStream().use { dtBuffer.asInputStream().copyTo(it) } }
                binaryFileContent.map { dtBuffer -> binFile.outputStream().use { dtBuffer.asInputStream().copyTo(it) } }
                saveBinaryFile(executionRequestForStandardSuites, propFile, binFile)
            }
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }

    /**
     * Controller to download standard test suites
     */
    @Suppress("TOO_LONG_FUNCTION")
    @PostMapping("/uploadStandardTestSuite")
    fun uploadStandardTestSuite() {
        readStandardTestSuitesFile(configProperties.reposFileName).forEach { (testSuiteUrl, testSuitePaths) ->
            log.info("Starting clone repository url=$testSuiteUrl for standard test suites")
            val tmpDir = generateDirectory(testSuiteUrl)
            cloneFromGit(GitDto(testSuiteUrl), tmpDir)?.use {
                Flux.fromIterable(testSuitePaths).flatMap { testRootPath ->
                    log.info("Starting to discover root test config for test root $testRootPath")
                    val testResourcesRootAbsolutePath = tmpDir.resolve(testRootPath).absolutePath
                    val rootTestConfig = testDiscoveringService.getRootTestConfig(testResourcesRootAbsolutePath)
                    println("rootTestConfig.location: " + rootTestConfig.location)
                    log.info("Starting to discover standard test suites for config test root $testRootPath in $testResourcesRootAbsolutePath")
                    val tests = testDiscoveringService.getAllTestSuites(null, rootTestConfig, "stub")
                    log.info("Test suites size = ${tests.size}")
                    log.info("Starting to save new test suites for config test root $testRootPath")
                    webClientBackend.makeRequest(BodyInserters.fromValue(tests), "/saveTestSuites") {
                        it.bodyToMono<List<TestSuite>>()
                    }
                        .flatMap { testSuites ->
                            log.info("Starting to save new tests for root test config $testRootPath")
                            webClientBackend.makeRequest(
                                BodyInserters.fromValue(
                                    testDiscoveringService.getAllTests(
                                        rootTestConfig,
                                        testSuites
                                    )
                                ),
                                "/initializeTests"
                            ) { it.toBodilessEntity() }
                        }
                }
                    .doOnError {
                        log.error("Error to update test with url=$testSuiteUrl, path=$testSuitePaths")
                    }
                    .collect(Collectors.toList())
                    .subscribe()
            }
        }
    }

    private fun cloneFromGit(gitDto: GitDto, tmpDir: File): Git? {
        val userCredentials = if (gitDto.username != null && gitDto.password != null) {
            UsernamePasswordCredentialsProvider(gitDto.username, gitDto.password)
        } else {
            CredentialsProvider.getDefault()
        }
        return Git.cloneRepository()
            .setURI(gitDto.url)
            .setCredentialsProvider(userCredentials)
            .setDirectory(tmpDir)
            .call()
    }

    @Suppress(
        "TYPE_ALIAS",
        "TOO_LONG_FUNCTION",
        "TOO_MANY_LINES_IN_LAMBDA",
        "UnsafeCallOnNullableType")
    private fun downLoadRepository(executionRequest: ExecutionRequest): Mono<Pair<String, String>> {
        val gitDto = executionRequest.gitDto
        val project = executionRequest.project
        val tmpDir = generateDirectory(gitDto.url)
        val userCredentials = if (gitDto.username != null && gitDto.password != null) {
            UsernamePasswordCredentialsProvider(gitDto.username, gitDto.password)
        } else {
            CredentialsProvider.getDefault()
        }
        return Mono.fromCallable {
            cloneFromGit(gitDto, tmpDir)?.use { git ->
                executionRequest.gitDto.hash?.let { hash ->
                    git.checkout().setName(hash).call()
                }
                val version = git.log().call().first()
                    .name
                log.info("Cloned repository ${gitDto.url}, head is at $version")
                return@fromCallable tmpDir.relativeTo(File(configProperties.repository)).normalize().path to version
            }
        }
            .onErrorResume { exception ->
                tmpDir.deleteRecursively()
                when (exception) {
                    is InvalidRemoteException,
                    is TransportException,
                    is GitAPIException -> log.warn("Error with git API while cloning ${gitDto.url} repository", exception)
                    else -> log.warn("Cloning ${gitDto.url} repository failed", exception)
                }
                webClientBackend.makeRequest(
                    BodyInserters.fromValue(ExecutionUpdateDto(executionRequest.executionId!!, ExecutionStatus.ERROR)),
                    "/updateExecution"
                ) { it.toEntity<HttpStatus>() }.flatMap {
                    Mono.error(exception)
                }
            }
    }

    private fun saveBinaryFile(
        executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        propertyFile: File,
        binFile: File,
    ): Mono<StatusResponse> {
        val tmpDir = generateDirectory(binFile.name)
        val pathToProperties = tmpDir.resolve(propertyFile.name)
        propertyFile.copyTo(pathToProperties)
        propertyFile.delete()
        binFile.copyTo(tmpDir.resolve(binFile.name))
        binFile.delete()
        val project = executionRequestForStandardSuites.project
        val propertiesRelativePath = pathToProperties.relativeTo(tmpDir).name
        // todo: what's with version?
        return updateExecution(executionRequestForStandardSuites.project, tmpDir.name, binFile.name).flatMap { execution ->
            sendToBackendAndOrchestrator(
                execution,
                project,
                propertiesRelativePath,
                tmpDir.relativeTo(File(configProperties.repository)).normalize().path,
                executionRequestForStandardSuites.testsSuites.map {
                    TestSuiteDto(
                        TestSuiteType.STANDARD,
                        it,
                        project,
                        propertiesRelativePath
                    )
                }
            )
        }
    }

    private fun generateDirectory(dirName: String): File {
        val hashName = dirName.hashCode()
        val tmpDir = File("${configProperties.repository}/$hashName")
        if (tmpDir.exists()) {
            tmpDir.deleteRecursively()
            log.info("For $dirName file: dir $tmpDir was deleted")
        }
        tmpDir.mkdirs()
        log.info("For $dirName repository: dir $tmpDir was created")
        return tmpDir
    }

    /**
     * Note: We have not null list of TestSuite only, if execute type is STANDARD ()
     *
     * - Post request to backend to create PENDING executions
     * - Discover all test suites in the cloned project
     * - Post request to backend to save all test suites
     * - Discover all tests in the cloned project
     * - Post request to backend to save all tests and create TestExecutions for them
     * - Send a request to orchestrator to initialize agents and start tests execution
     */
    @Suppress(

        "LongParameterList",
        "TOO_MANY_PARAMETERS",
        "UnsafeCallOnNullableType"
    )
    private fun sendToBackendAndOrchestrator(
        execution: Execution,
        project: Project,
        propertiesRelativePath: String,
        projectRootRelativePath: String,
        testSuiteDtos: List<TestSuiteDto>?,
    ): Mono<StatusResponse> {
        val executionType = execution.type
        testSuiteDtos?.let {
            require(executionType == ExecutionType.STANDARD) { "Test suites shouldn't be provided unless ExecutionType is STANDARD (actual: $executionType)" }
        } ?: require(executionType == ExecutionType.GIT) { "Test suites are not provided, but should for executionType=$executionType" }

        return if (executionType == ExecutionType.GIT) {
            prepareForExecutionFromGit(project, execution, propertiesRelativePath, projectRootRelativePath)
        } else {
            prepareExecutionForStandard(testSuiteDtos!!)
        }
            .then(initializeAgents(execution))
            .onErrorResume { ex ->
                log.error(
                    "Error during preprocessing, will mark execution.id=${execution.id} as failed; error: ",
                    ex
                )
                webClientBackend.makeRequest(
                    BodyInserters.fromValue(ExecutionUpdateDto(execution.id!!, ExecutionStatus.ERROR)),
                    "/updateExecution"
                ) { it.toEntity<HttpStatus>() }
            }
    }

    private fun updateExecution(project: Project, projectRootRelativePath: String, executionVersion: String): Mono<Execution> {
        val executionUpdate = ExecutionInitializationDto(project, "ALL", projectRootRelativePath, executionVersion)
        return webClientBackend.makeRequest(BodyInserters.fromValue(executionUpdate), "/updateNewExecution") {
            it.onStatus({ status -> status != HttpStatus.OK }) { clientResponse ->
                log.error("Error when making update to execution fro project id = ${project.id} ${clientResponse.statusCode()}")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Execution not found"
                )
            }
            it.bodyToMono()
        }

    }

    @Suppress("UnsafeCallOnNullableType")
    private fun prepareForExecutionFromGit(project: Project,
                                           execution: Execution,
                                           propertiesRelativePath: String,
                                           projectRootRelativePath: String): Mono<EmptyResponse> = Mono.fromCallable {
        val testResourcesRootAbsolutePath =
                getTestResourcesRootAbsolutePath(propertiesRelativePath, projectRootRelativePath)
        println("\n\ntestResourcesRootAbsolutePath: ${testResourcesRootAbsolutePath}\n\n")
        testDiscoveringService.getRootTestConfig(testResourcesRootAbsolutePath)
    }
        .log()
        .zipWhen { rootTestConfig ->
            discoverAndSaveTestSuites(project, rootTestConfig, propertiesRelativePath)
        }
        .flatMap { (rootTestConfig, testSuites) ->
            initializeTests(testSuites, rootTestConfig, execution.id!!)
        }
    
    private fun prepareExecutionForStandard(testSuiteDtos: List<TestSuiteDto>): Mono<List<TestSuite>> {
        return webClientBackend.makeRequest<List<TestSuiteDto>?, List<TestSuite>>(
            BodyInserters.fromValue(
                testSuiteDtos
            ), "/saveTestSuites"
        ) {
            it.bodyToMono()
        }
        // fixme: should also initialize tests from standard suites
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun getTestResourcesRootAbsolutePath(propertiesRelativePath: String,
                                                 projectRootRelativePath: String): String {
        // TODO 1) FILE NOT FOUND? - create logic
        // TODO 2) what if path - to dir?
        val propertiesFile = File(configProperties.repository, projectRootRelativePath)
            .resolve(propertiesRelativePath)
        val saveProperties: SaveProperties = decodeFromPropertiesFile<SaveProperties>(propertiesFile)
            .mergeConfigWithPriorityToThis(defaultConfig())
        return propertiesFile.parentFile
            .resolve(saveProperties.testRootPath!!)
            .absolutePath
    }

    private fun discoverAndSaveTestSuites(project: Project?,
                                          rootTestConfig: TestConfig,
                                          propertiesRelativePath: String): Mono<List<TestSuite>> {
        val testSuites: List<TestSuiteDto> = testDiscoveringService.getAllTestSuites(project, rootTestConfig, propertiesRelativePath)
        return webClientBackend.makeRequest(BodyInserters.fromValue(testSuites), "/saveTestSuites") {
            it.bodyToMono()
        }
    }

    /**
     * Discover tests and send them to backend
     */
    private fun initializeTests(testSuites: List<TestSuite>,
                                rootTestConfig: TestConfig,
                                executionId: Long) = webClientBackend.makeRequest(
        BodyInserters.fromValue(testDiscoveringService.getAllTests(rootTestConfig, testSuites)),
        "/initializeTests?executionId=$executionId"
    ) {
        it.toBodilessEntity()
    }

    /**
     * Post request to orchestrator to initiate its work
     */
    private fun initializeAgents(execution: Execution) = webClientOrchestrator.makeRequest(
        BodyInserters.fromValue(execution),
        "/initializeAgents"
    ) {
        it.toEntity<HttpStatus>()
    }

    private fun <M, T> WebClient.makeRequest(
        body: BodyInserter<M, ReactiveHttpOutputMessage>,
        uri: String,
        toBody: (WebClient.ResponseSpec) -> Mono<T>
    ): Mono<T> {
        val responseSpec = this
            .post()
            .uri(uri)
            .body(body)
            .retrieve()
            .onStatus({status -> status != HttpStatus.OK }) { clientResponse ->
                log.error("Error when making request to $uri: ${clientResponse.statusCode()}")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Upstream request error"
                )
            }
        return toBody(responseSpec).log()
    }
}

/**
 * @param name file name to read
 * @return map repository to paths to test configs
 */
fun readStandardTestSuitesFile(name: String) =
        ClassPathResource(name)
            .file
            .readText()
            .lines()
            .associate {
                val splitRow = it.split("\\s".toRegex())
                require(splitRow.size == 2)
                splitRow.first() to splitRow[1].split(";")
            }
