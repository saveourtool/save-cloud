package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.preprocessor.Response
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.service.TestDiscoveringService
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
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

import java.io.File
import java.time.LocalDateTime
import java.util.Properties

import kotlin.io.path.ExperimentalPathApi
import kotlinx.serialization.properties.decodeFromMap

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

    @Suppress("TooGenericExceptionCaught", "TOO_LONG_FUNCTION")
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
        val pathToProperties = tmpDir.path + File.separator + propertyFile.name
        propertyFile.copyTo(File(pathToProperties))
        binFile.copyTo(File(tmpDir.path + File.separator + binFile.name))
        val project = executionRequestForStandardSuites.project
        sendToBackendAndOrchestrator(
            project,
            pathToProperties,
            tmpDir.relativeTo(File(configProperties.repository)).normalize().path,
            ExecutionType.STANDARD,
            executionRequestForStandardSuites.testsSuites.map { TestSuiteDto(TestSuiteType.STANDARD, it, project, pathToProperties) }
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
     */
    @Suppress(
        "LongMethod",
        "ThrowsCount",
        "TooGenericExceptionCaught",
        "TOO_LONG_FUNCTION",
        "LOCAL_VARIABLE_EARLY_DECLARATION")
    private fun sendToBackendAndOrchestrator(
        project: Project,
        propertiesRelativePath: String,
        resourcesRootPath: String,
        executionType: ExecutionType,
        testSuitesDto: List<TestSuiteDto>?
    ) {
        testSuitesDto?.let {
            require(executionType == ExecutionType.STANDARD)
        } ?: require(executionType == ExecutionType.GIT)
        val execution = Execution(project, LocalDateTime.now(), LocalDateTime.now(),
            ExecutionStatus.PENDING, "1", resourcesRootPath, 0, configProperties.executionLimit, executionType)
        log.debug("Knock-Knock Backend")
        makeRequest(BodyInserters.fromValue(execution), "/createExecution") { it.bodyToMono(Long::class.java) }
            .doOnNext { executionId ->
                val rawProperties = Properties().apply {
                    load(File(configProperties.repository, propertiesRelativePath).inputStream())
                }
                val saveProperties: SaveProperties = kotlinx.serialization.properties.Properties.decodeFromMap(rawProperties as Map<String, Any>)
                val testResourcesRootPath = File(configProperties.repository, saveProperties.testConfigPath!!).absolutePath
                val testSuites: List<TestSuiteDto> = try {
                    testDiscoveringService.getAllTestSuites(project, testResourcesRootPath)
                } catch (iae: IllegalArgumentException) {
                    log.error("Couldn't discover test suites, aborting: ", iae)
                    return@doOnNext
                }
                makeRequest(BodyInserters.fromValue(testSuites), "/saveTestSuites") {
                    it.bodyToMono<List<TestSuite>>()
                }
                    .doOnNext { testSuiteList ->
                        makeRequest(
                            BodyInserters.fromValue(testDiscoveringService.getAllTests(testResourcesRootPath, testSuiteList)),
                            "/initializeTests?executionId=$executionId"
                        ) {
                            it.toBodilessEntity()
                        }
                            .doOnNext {
                                // Post request to orchestrator to initiate its work
                                log.debug("Knock-Knock Orchestrator")
                                webClientOrchestrator
                                    .post()
                                    .uri("/initializeAgents")
                                    .body(BodyInserters.fromValue(execution.also { it.id = executionId }))
                                    .retrieve()
                                    .toEntity(HttpStatus::class.java)
                                    .subscribe()
                            }.subscribe()
                    }.subscribe()
            }.subscribe()
    }

    private fun <M, T> makeRequest(
        body: BodyInserter<M, ReactiveHttpOutputMessage>,
        uri: String,
        toBody: (WebClient.ResponseSpec) -> Mono<T>
    ): Mono<T> {
        val responseSpec = webClientBackend
            .post()
            .uri(uri)
            .body(body)
            .retrieve()
            .onStatus({status -> status != HttpStatus.OK }) { clientResponse ->
                log.error("Backend internal error: ${clientResponse.statusCode()}")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Backend internal error"
                )
            }
        return toBody(responseSpec)
    }
}
