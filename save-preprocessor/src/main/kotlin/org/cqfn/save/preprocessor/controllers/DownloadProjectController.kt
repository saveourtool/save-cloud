package org.cqfn.save.preprocessor.controllers

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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.time.LocalDateTime

/**
 * A Spring controller for git project downloading
 *
 * @property configProperties config properties
 */
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

    @Suppress("TooGenericExceptionCaught", "TOO_LONG_FUNCTION")
    private fun downLoadRepository(executionRequest: ExecutionRequest) {
        val gitRepository = executionRequest.gitRepository
        val project = executionRequest.project
        val urlHash = gitRepository.url.hashCode()
        val tmpDir = File("${configProperties.repository}/$urlHash")
        if (tmpDir.exists()) {
            tmpDir.deleteRecursively()
            log.info("For ${gitRepository.url} repository: dir $urlHash was deleted")
        }
        tmpDir.mkdirs()
        log.info("For ${gitRepository.url} repository: dir $urlHash was created")
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
                    sendToBackendAndOrchestrator(project, tmpDir.relativeTo(File(configProperties.repository)).normalize().path)
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

    @Suppress("LongMethod", "ThrowsCount", "TooGenericExceptionCaught", "TOO_LONG_FUNCTION")
    private fun sendToBackendAndOrchestrator(project: Project, path: String) {
        val execution = Execution(project, LocalDateTime.now(), LocalDateTime.now(),
            ExecutionStatus.PENDING, "1", path, 0, configProperties.executionLimit)
        var execId: Long
        log.debug("Knock-Knock Backend")
        webClientBackend
            .post()
            .uri("/createExecution")
            .body(BodyInserters.fromValue(execution))
            .retrieve()
            .onStatus({status -> status != HttpStatus.OK }) {
                log.error("Backend internal error: ${it.statusCode()}")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Backend internal error"
                )
            }
            .bodyToMono(Long::class.java)
            .doOnNext {
                execId = it
                webClientBackend.
                    post()
                    .uri("/saveTestSuites")
                    .body(BodyInserters.fromValue(getAllTestSuits(project)))
                    .retrieve()
                    .onStatus({status -> status != HttpStatus.OK }) {
                    log.error("Backend internal error: ${it.statusCode()}")
                    throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Backend internal error"
                    )
                }
                .bodyToMono<List<TestSuite>>()
                .doOnNext {
                    webClientBackend.
                    post()
                        .uri("/initializeTests")
                        .body(BodyInserters.fromValue(getAllTests(path, it)))
                        .retrieve()
                        .onStatus({status -> status != HttpStatus.OK }) {
                            log.error("Backend internal error: ${it.statusCode()}")
                            throw ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Backend internal error"
                            )
                        }
                        .toBodilessEntity()
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

    private fun getAllTestSuits(project: Project): List<TestSuiteDto> {
        return listOf(TestSuiteDto(TestSuiteType.PROJECT, "test", project))
    }

    private fun getAllTests(path: String, testSuites: List<TestSuite>): List<TestDto> {
        return File(path)
            .walkTopDown()
            .filter { it.isFile }
            .map {
                TestDto(it.path, it.toHash(), testSuites[0].id ?: 1)
            }
            .toList()
    }
}
