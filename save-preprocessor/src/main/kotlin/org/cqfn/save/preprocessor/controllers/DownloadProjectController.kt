@file:Suppress("UNUSED_IMPORT")

package org.cqfn.save.preprocessor.controllers

// for these imports we need to suppress UNUSED_IMPORT until https://github.com/cqfn/diKTat/issues/837
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
import org.cqfn.save.preprocessor.Response
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.service.TestDiscoveringService
import org.cqfn.save.preprocessor.utils.decodeFromPropertiesFile
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteRepo
import org.cqfn.save.testsuite.TestSuiteType

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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

import kotlin.io.path.ExperimentalPathApi

/**
 * A Spring controller for git project downloading
 *
 * @property configProperties config properties
 */
@ExperimentalPathApi
@RestController
class DownloadProjectController(private val configProperties: ConfigProperties) {
    private val log = LoggerFactory.getLogger(DownloadProjectController::class.java)
    private val webClientBackend = WebClient.create(configProperties.backend)
    private val webClientOrchestrator = WebClient.create(configProperties.orchestrator)
    @Autowired private lateinit var testDiscoveringService: TestDiscoveringService

    /**
     * @param executionRequest - Dto of repo information to clone and project info
     * @return response entity with text
     */
    @Suppress("TooGenericExceptionCaught")
    @PostMapping(value = ["/upload"])
    fun upload(@RequestBody executionRequest: ExecutionRequest): Response = Mono.just(ResponseEntity("Clone pending", HttpStatus.ACCEPTED))
        .subscribeOn(Schedulers.boundedElastic())
        .also {
            it.subscribe {
                downLoadRepository(executionRequest)
            }
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
        .subscribeOn(Schedulers.boundedElastic())
        .also {
            it.flatMap {
                val binFile = File("program")
                val propFile = File("save.properties")
                Mono.zip(
                    propertyFile.flatMapMany { it.content() }.collectList(),
                    binaryFile.flatMapMany { it.content() }.collectList()
                ).map { (propertyFileContent, binaryFileContent) ->
                    propertyFileContent.map { dtBuffer -> propFile.outputStream().use { dtBuffer.asInputStream().copyTo(it) } }
                    binaryFileContent.map { dtBuffer -> binFile.outputStream().use { dtBuffer.asInputStream().copyTo(it) } }
                    saveBinaryFile(executionRequestForStandardSuites, propFile, binFile)
                }
            }
                .subscribe()
        }

    /**
     * Controller to download standard test suites
     *
     * @param testSuiteRepo info about test suites
     * @return response entity with text
     */
    @PostMapping("/uploadStandardTestSuite")
    fun uploadStandardTestSuite(@RequestBody testSuiteRepo: TestSuiteRepo) = Mono.just(ResponseEntity("Clone pending", HttpStatus.ACCEPTED))
        .subscribeOn(Schedulers.boundedElastic())
        .also {
            val tmpDir = generateDirectory(testSuiteRepo.gitUrl)
            cloneFromGit(GitDto(testSuiteRepo.gitUrl), tmpDir).use {
                Flux.fromIterable(testSuiteRepo.propertiesRelativePaths).flatMap { propPath ->
                    log.info("Starting to discover root test config")
                    val testResourcesRootAbsolutePath = File(tmpDir.relativeTo(File(configProperties.repository)).normalize().path, propPath).absolutePath
                    val rootTestConfig = testDiscoveringService.getRootTestConfig(testResourcesRootAbsolutePath)
                    log.info("Starting to discover standard test suites")
                    val tests = testDiscoveringService.getAllTestSuites(null, rootTestConfig, "stub")
                    log.info("Test suites size = ${tests.size}")
                    log.info("Starting to save new test suites")
                    webClientBackend.makeRequest(BodyInserters.fromValue(tests), "/saveTestSuites") {
                        it.bodyToMono<List<TestSuite>>()
                    }
                        .flatMap { testSuites ->
                            log.info("Starting to save new tests")
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
                    .subscribe()
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
        "TooGenericExceptionCaught",
        "TOO_LONG_FUNCTION",
        "TOO_MANY_LINES_IN_LAMBDA",
        "UnsafeCallOnNullableType")
    private fun downLoadRepository(executionRequest: ExecutionRequest) {
        val gitDto = executionRequest.gitDto
        val project = executionRequest.project
        val tmpDir = generateDirectory(gitDto.url)
        val userCredentials = if (gitDto.username != null && gitDto.password != null) {
            UsernamePasswordCredentialsProvider(gitDto.username, gitDto.password)
        } else {
            CredentialsProvider.getDefault()
        }
        try {
            cloneFromGit(gitDto, tmpDir)?.use {
                log.info("Repository cloned: ${gitDto.url}")
                // Post request to backend to create PENDING executions
                sendToBackendAndOrchestrator(
                    project,
                    executionRequest.propertiesRelativePath,
                    tmpDir.relativeTo(File(configProperties.repository)).normalize().path,
                    ExecutionType.GIT,
                    it.log().call().first()
                        .name,
                    null
                )
            }
        } catch (exception: Exception) {
            tmpDir.deleteRecursively()
            when (exception) {
                is InvalidRemoteException,
                is TransportException,
                is GitAPIException -> log.warn("Error with git API while cloning ${gitDto.url} repository", exception)
                else -> log.warn("Cloning ${gitDto.url} repository failed", exception)
            }
            webClientBackend.makeRequest(
                BodyInserters.fromValue(ExecutionUpdateDto(executionRequest.executionId!!, ExecutionStatus.ERROR)), "/updateExecution"
            ) { it.toEntity<HttpStatus>() }
        }
    }

    private fun saveBinaryFile(
        executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        propertyFile: File,
        binFile: File,
    ) {
        val tmpDir = generateDirectory(binFile.name)
        val pathToProperties = tmpDir.resolve(propertyFile.name)
        propertyFile.copyTo(pathToProperties)
        binFile.copyTo(tmpDir.resolve(binFile.name))
        val project = executionRequestForStandardSuites.project
        val propertiesRelativePath = pathToProperties.relativeTo(tmpDir).name
        sendToBackendAndOrchestrator(
            project,
            propertiesRelativePath,
            tmpDir.relativeTo(File(configProperties.repository)).normalize().path,
            ExecutionType.STANDARD,
            binFile.name,
            executionRequestForStandardSuites.testsSuites.map { TestSuiteDto(TestSuiteType.STANDARD, it, project, propertiesRelativePath) }
        )
    }

    private fun generateDirectory(dirName: String): File {
        val hashName = dirName.hashCode()
        val tmpDir = File("${configProperties.repository}/$hashName")
        if (tmpDir.exists()) {
            tmpDir.deleteRecursively()
            log.info("For $dirName file: dir $hashName was deleted")
        }
        tmpDir.mkdirs()
        log.info("For $dirName repository: dir $hashName was created")
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
        "LongMethod",
        "ThrowsCount",
        "TooGenericExceptionCaught",
        "TOO_LONG_FUNCTION",
        "LOCAL_VARIABLE_EARLY_DECLARATION",
        "LongParameterList",
        "TOO_MANY_PARAMETERS",
        "UnsafeCallOnNullableType"
    )
    private fun sendToBackendAndOrchestrator(
        project: Project,
        propertiesRelativePath: String,
        projectRootRelativePath: String,
        executionType: ExecutionType,
        executionVersion: String,
        testSuiteDtos: List<TestSuiteDto>?
    ) {
        testSuiteDtos?.let {
            require(executionType == ExecutionType.STANDARD) { "Test suites shouldn't be provided unless ExecutionType is STANDARD (actual: $executionType)" }
        } ?: require(executionType == ExecutionType.GIT) { "Test suites are not provided, but should for executionType=$executionType" }

        val executionUpdate = ExecutionInitializationDto(project, "ALL", projectRootRelativePath, configProperties.executionLimit, executionVersion)
        webClientBackend.makeRequest(BodyInserters.fromValue(executionUpdate), "/updateNewExecution") {
            it.onStatus({status -> status != HttpStatus.OK }) { clientResponse ->
                log.error("Error when making update to execution fro project id = ${project.id} ${clientResponse.statusCode()}")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Execution not found")
            }
            it.bodyToMono<Execution>()
        }
            .flatMap { execution ->
                if (executionType == ExecutionType.GIT) {
                    prepareForExecutionFromGit(project, execution, propertiesRelativePath, projectRootRelativePath)
                } else {
                    prepareExecutionForStandard(project, execution, testSuiteDtos!!)
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
            .subscribe()
    }

    @Suppress("TYPE_ALIAS", "UnsafeCallOnNullableType")
    private fun prepareForExecutionFromGit(project: Project,
                                           execution: Execution,
                                           propertiesRelativePath: String,
                                           projectRootRelativePath: String): Mono<*> = Mono.fromCallable {
        val testResourcesRootAbsolutePath =
                getTestResourcesRootAbsolutePath(propertiesRelativePath, projectRootRelativePath)
        testDiscoveringService.getRootTestConfig(testResourcesRootAbsolutePath)
    }
        .log()
        .zipWhen { rootTestConfig ->
            discoverAndSaveTestSuites(project, rootTestConfig, propertiesRelativePath)
        }
        .flatMap { (rootTestConfig, testSuites) ->
            initializeTests(testSuites, rootTestConfig, execution.id!!)
        }
    
    private fun prepareExecutionForStandard(project: Project,
                                            execution: Execution,
                                            testSuiteDtos: List<TestSuiteDto>): Mono<*> {
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
