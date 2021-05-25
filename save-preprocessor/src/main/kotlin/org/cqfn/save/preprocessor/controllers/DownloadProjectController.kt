package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.entities.BinaryExecutionRequest
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.preprocessor.Response
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.utils.toHash
import org.cqfn.save.test.TestDto
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
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
     * @param binaryExecutionRequest - Dto of binary file, test suites names and project info
     * @param property
     * @param binaryFile
     * @return response entity with text
     */
    @Suppress("TooGenericExceptionCaught")
    @PostMapping(value = ["/uploadBin"], consumes = ["multipart/form-data"])
    fun uploadBin(
        @RequestPart binaryExecutionRequest: BinaryExecutionRequest,
        @RequestPart("property", required = true) property: Mono<File>,
        @RequestPart("binFile", required = true) binaryFile: Mono<File>,
    ) = Mono.just(ResponseEntity("Clone pending", HttpStatus.ACCEPTED))
        .subscribeOn(Schedulers.boundedElastic())
        .also {
            it.subscribe {
                binaryFile.subscribe { binFile ->
                    property.subscribe { propFile ->
                        saveBinaryFile(binaryExecutionRequest, propFile, binFile)
                    }
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
                    // Fixme: need to initialize test suite ids
                    sendToBackendAndOrchestrator(
                        project,
                        executionRequest.propertiesRelativePath,
                        tmpDir.relativeTo(File(configProperties.repository)).normalize().path,
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
        binaryExecutionRequest: BinaryExecutionRequest,
        property: File,
        binFile: File,
    ) {
        val tmpDir = generateDirectory(binFile.name.hashCode(), binFile.name)
        val pathToProperties = tmpDir.path + File.separator + property.name
        property.copyTo(File(pathToProperties))
        binFile.copyTo(File(tmpDir.path + File.separator + binFile.name))
        val project = binaryExecutionRequest.project
        sendToBackendAndOrchestrator(
            project,
            pathToProperties,
            tmpDir.relativeTo(File(configProperties.repository)).normalize().path,
            binaryExecutionRequest.testsSuites.map { TestSuiteDto(TestSuiteType.STANDARD, it, project, pathToProperties) }
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
        testSuitesDto: List<TestSuiteDto>?
    ) {
        val execution = Execution(project, LocalDateTime.now(), LocalDateTime.now(),
            ExecutionStatus.PENDING, "1", resourcesRootPath, 0, configProperties.executionLimit)
        var execId: Long
        log.debug("Knock-Knock Backend")
        makeRequest(BodyInserters.fromValue(execution), "/createExecution") { it.bodyToMono(Long::class.java) }
            .doOnNext { executionId ->
                execId = executionId
                makeRequest(BodyInserters.fromValue(testSuitesDto ?: getAllTestSuites(project, propertiesRelativePath)), "/saveTestSuites") { it.bodyToMono<List<TestSuite>>() }
                    .doOnNext { testSuiteList ->
                        makeRequest(BodyInserters.fromValue(getAllTests(resourcesRootPath, testSuiteList)), "/initializeTests?executionId=$executionId") { it.toBodilessEntity() }
                            .doOnNext {
                                // Post request to orchestrator to initiate its work
                                log.debug("Knock-Knock Orchestrator")
                                webClientOrchestrator
                                    .post()
                                    .uri("/initializeAgents")
                                    .body(BodyInserters.fromValue(execution.also { it.id = execId }))
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

    private fun getAllTestSuites(project: Project, propertiesRelativePath: String) =
            listOf(TestSuiteDto(TestSuiteType.PROJECT, "test", project, propertiesRelativePath))

    private fun getAllTests(path: String, testSuites: List<TestSuite>): List<TestDto> {
        // todo Save should find and create correct TestDtos. Not it's just a stub
        return File(configProperties.repository, path)
            .walkTopDown()
            .filter { it.isFile }
            .map {
                TestDto(it.path, testSuites[0].id ?: 1, it.toHash())
            }
            .toList()
    }
}
