package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.execution.ExecutionUpdateDto
import org.cqfn.save.preprocessor.Response
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.ResponseEntity
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
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

import java.io.File
import java.time.LocalDateTime

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
    @PostMapping(value = ["/uploadBin"], consumes = ["multipart/form-data"])
    fun uploadBin(
        @RequestPart executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        @RequestPart("property", required = true) propertyFile: Mono<File>,
        @RequestPart("binFile", required = true) binaryFile: Mono<File>,
    ) = Mono.just(ResponseEntity("Clone pending", HttpStatus.ACCEPTED))
        .subscribeOn(Schedulers.boundedElastic())
        .also {
            it.subscribe {
                Mono.zip(propertyFile, binaryFile).subscribe {
                    saveBinaryFile(executionRequestForStandardSuites, it.t1, it.t2)
                }
            }
        }

    @Suppress(
        "TooGenericExceptionCaught",
        "TOO_LONG_FUNCTION",
        "TOO_MANY_LINES_IN_LAMBDA")
    private fun downLoadRepository(executionRequest: ExecutionRequest) {
        val gitRepository = executionRequest.gitRepository
        val project = executionRequest.project
        val tmpDir = generateDirectory(gitRepository.url.hashCode(), gitRepository.url)
        val userCredentials = if (gitRepository.username != null && gitRepository.password != null) {
            UsernamePasswordCredentialsProvider(gitRepository.username, gitRepository.password)
        } else {
            CredentialsProvider.getDefault()
        }
        try {
            Git.cloneRepository()
                .setURI(gitRepository.url)
                .setCredentialsProvider(userCredentials)
                .setDirectory(tmpDir)
                .call().use {
                    log.info("Repository cloned: ${gitRepository.url}")
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
                is GitAPIException -> log.warn("Error with git API while cloning ${gitRepository.url} repository", exception)
                else -> log.warn("Cloning ${gitRepository.url} repository failed", exception)
            }
        }
    }

    private fun saveBinaryFile(
        executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        propertyFile: File,
        binFile: File,
    ) {
        val tmpDir = generateDirectory(binFile.name.hashCode(), binFile.name)
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
            executionRequestForStandardSuites.testsSuites.map { TestSuiteDto(TestSuiteType.STANDARD, it, project, propertiesRelativePath) }
        )
    }

    private fun generateDirectory(hashName: Int, dirName: String): File {
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
        "TOO_MANY_PARAMETERS")
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

        val execution = Execution(project, LocalDateTime.now(), LocalDateTime.now(),
            ExecutionStatus.PENDING, "ALL", projectRootRelativePath, 0, configProperties.executionLimit, executionType, executionVersion)
        webClientBackend.makeRequest(BodyInserters.fromValue(execution), "/createExecution") { it.bodyToMono<Long>() }
            .flatMap { executionId ->
                Mono.fromCallable {
                    getTestResourcesRootAbsolutePath(propertiesRelativePath, projectRootRelativePath)
                }
                    .flatMap { testResourcesRootAbsolutePath ->
                        discoverAndSaveTestSuites(project, testResourcesRootAbsolutePath, propertiesRelativePath)
                            .flatMap { testSuites ->
                                initializeTests(testSuites, testResourcesRootAbsolutePath, executionId)
                                    .then(initializeAgents(execution, executionId))
                            }
                    }
                    .onErrorResume { ex ->
                        log.error("Error during resources discovering, will mark execution.id=$executionId as failed; error: ", ex)
                        webClientBackend.makeRequest(
                            BodyInserters.fromValue(ExecutionUpdateDto(executionId, ExecutionStatus.ERROR)), "/updateExecution"
                        ) { it.toEntity<HttpStatus>() }
                    }
            }
            .subscribe()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun getTestResourcesRootAbsolutePath(propertiesRelativePath: String,
                                                 projectRootRelativePath: String): String {
        val propertiesFile = File(configProperties.repository, projectRootRelativePath)
            .resolve(propertiesRelativePath)
        val saveProperties: SaveProperties = decodeFromPropertiesFile(propertiesFile)
        return propertiesFile.parentFile
            .resolve(saveProperties.testConfigPath!!)
            .absolutePath
    }

    private fun discoverAndSaveTestSuites(project: Project,
                                          testResourcesRootAbsolutePath: String,
                                          propertiesRelativePath: String): Mono<List<TestSuite>> {
        val testSuites: List<TestSuiteDto> = testDiscoveringService.getAllTestSuites(project, testResourcesRootAbsolutePath, propertiesRelativePath)
        return webClientBackend.makeRequest(BodyInserters.fromValue(testSuites), "/saveTestSuites") {
            it.bodyToMono()
        }
    }

    /**
     * Discover tests and send them to backend
     */
    private fun initializeTests(testSuites: List<TestSuite>,
                                testResourcesRootPath: String,
                                executionId: Long) = webClientBackend.makeRequest(
        BodyInserters.fromValue(testDiscoveringService.getAllTests(testResourcesRootPath, testSuites)),
        "/initializeTests?executionId=$executionId"
    ) {
        it.toBodilessEntity()
    }

    /**
     * Post request to orchestrator to initiate its work
     */
    private fun initializeAgents(execution: Execution,
                                 executionId: Long) = webClientOrchestrator.makeRequest(
        BodyInserters.fromValue(execution.also { it.id = executionId }),
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
