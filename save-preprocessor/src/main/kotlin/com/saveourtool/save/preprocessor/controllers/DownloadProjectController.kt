package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.ExecutionInitializationDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.preprocessor.StatusResponse
import com.saveourtool.save.preprocessor.TextResponse
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.service.TestsPreprocessorToBackendBridge
import com.saveourtool.save.preprocessor.utils.*
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.info

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
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
import reactor.netty.http.client.HttpClientRequest

import java.time.Duration

import kotlin.io.path.ExperimentalPathApi

typealias Status = Mono<ResponseEntity<HttpStatus>>

/**
 * A Spring controller for git project downloading
 *
 * @property configProperties config properties
 */
@OptIn(ExperimentalPathApi::class)
@RestController
class DownloadProjectController(
    private val configProperties: ConfigProperties,
    private val objectMapper: ObjectMapper,
    kotlinSerializationWebClientCustomizer: WebClientCustomizer,
    private val testSuitesPreprocessorController: TestSuitesPreprocessorController,
    private val testsPreprocessorToBackendBridge: TestsPreprocessorToBackendBridge,
) {
    private val log = LoggerFactory.getLogger(DownloadProjectController::class.java)
    private val webClientBackend = WebClient.builder()
        .baseUrl(configProperties.backend)
        .apply(kotlinSerializationWebClientCustomizer::customize)
        .build()
    private val webClientOrchestrator = WebClient.builder()
        .baseUrl(configProperties.orchestrator)
        .codecs {
            it.defaultCodecs().multipartCodecs().encoder(Jackson2JsonEncoder(objectMapper))
        }
        .apply(kotlinSerializationWebClientCustomizer::customize)
        .build()
    private val scheduler = Schedulers.boundedElastic()

    /**
     * @param executionRequest Dto of repo information to clone and project info
     * @return response entity with text
     */
    @Suppress("TOO_LONG_FUNCTION")
    @PostMapping("/upload")
    fun upload(
        @RequestBody executionRequest: ExecutionRequest,
    ): Mono<TextResponse> = executionRequest.toAcceptedResponseMono()
        .doOnSuccess {
            Mono.fromCallable {
                val (selectedBranch, selectedVersion) = with(executionRequest.branchOrCommit) {
                    if (isNullOrBlank()) {
                        null to null
                    } else if (startsWith("origin/")) {
                        replaceFirst("origin/", "") to null
                    } else {
                        null to this
                    }
                }
                val branch = selectedBranch ?: executionRequest.gitDto.detectDefaultBranchName()
                val version = selectedVersion ?: executionRequest.gitDto.detectLatestSha1(branch)
                branch to version
            }
                .flatMapMany { (branch, version) ->
                    // search or create new test suites source by content
                    testsPreprocessorToBackendBridge.getOrCreateTestSuitesSource(
                        executionRequest.project.organization.name,
                        executionRequest.gitDto.url,
                        executionRequest.testRootPath,
                        branch,
                    ).mapToTestSuites(version)
                }
                .saveOnExecutionAndExecute(executionRequest)
                .subscribeOn(scheduler)
                .subscribe()
        }

    /**
     * @param executionRequestForStandardSuites Dto of binary file, test suites names and project info
     * @return response entity with text
     */
    @PostMapping(value = ["/uploadBin"])
    fun uploadBin(
        @RequestBody executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
    ) = executionRequestForStandardSuites.toAcceptedResponseMono()
        .doOnSuccess {
            testsPreprocessorToBackendBridge.getStandardTestSuitesSources()
                .flatMapMany { Flux.fromIterable(it) }
                .flatMap { testSuitesSource ->
                    testsPreprocessorToBackendBridge.listTestSuitesSourceVersions(testSuitesSource)
                        .map { keys ->
                            keys.maxByOrNull(TestSuitesSourceSnapshotKey::creationTimeInMills)?.version
                                ?: throw IllegalStateException("Failed to detect latest version for $testSuitesSource")
                        }
                        .flatMapMany { testSuitesSource.getTestSuites(it) }
                }
                .saveOnExecutionAndExecute(executionRequestForStandardSuites)
                .subscribeOn(scheduler)
                .subscribe()
        }

    private fun Mono<TestSuitesSourceDto>.mapToTestSuites(
        version: String,
    ): Flux<TestSuite> = flatMapMany { it.fetchAndGetTestSuites(version) }

    private fun TestSuitesSourceDto.fetchAndGetTestSuites(
        version: String,
    ): Flux<TestSuite> = testSuitesPreprocessorController.fetch(this, version)
        .flatMapMany { getTestSuites(version) }

    private fun TestSuitesSourceDto.getTestSuites(
        version: String,
    ): Flux<TestSuite> = testsPreprocessorToBackendBridge.getTestSuites(
        organizationName,
        name,
        version
    ).flatMapMany { Flux.fromIterable(it) }

    private fun Flux<TestSuite>.saveOnExecutionAndExecute(
        requestBase: ExecutionRequestBase,
    ) = this
        .filter(requestBase.getTestSuiteFilter())
        .collectList()
        .flatMap { testSuites ->
            require(testSuites.isNotEmpty()) {
                "No test suite is selected"
            }
            testSuites.map { it.source }
                .distinctBy { it.requiredId() }
                .also { sources ->
                    require(sources.size == 1) {
                        "Only a single test suites source is allowed for a run, but got: $sources"
                    }
                }
            val version = testSuites.map { it.version }
                .distinct()
                .also { versions ->
                    require(versions.size == 1) {
                        "Only a single version is supported, but got: $versions"
                    }
                }
                .single()
            updateExecution(
                requestBase,
                version,
                testSuites.map { it.requiredId() },
            )
        }
        .flatMap { it.executeTests() }

    /**
     * Accept execution rerun request
     *
     * @param executionId ID of [Execution]
     * @return status 202
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    @PostMapping("/rerunExecution")
    fun rerunExecution(@RequestParam("id") executionId: Long) = Mono.fromCallable {
        ResponseEntity("Clone pending", HttpStatus.ACCEPTED)
    }
        .doOnSuccess {
            updateExecutionStatus(executionId, ExecutionStatus.PENDING)
                .flatMap { cleanupInOrchestrator(executionId) }
                .flatMap { getExecution(executionId) }
                .doOnNext {
                    log.info { "Skip initializing tests for execution.id = ${it.id}: it's rerun" }
                }
                .flatMap { it.executeTests() }
                .subscribeOn(scheduler)
                .subscribe()
        }

    /**
     * Controller to download standard test suites
     *
     * @return Empty response entity
     */
    @Suppress("TOO_LONG_FUNCTION", "TYPE_ALIAS")
    @PostMapping("/uploadStandardTestSuite")
    fun uploadStandardTestSuite() =
            Mono.just(ResponseEntity("Upload standard test suites pending...\n", HttpStatus.ACCEPTED))
                .doOnSuccess {
                    testsPreprocessorToBackendBridge.getStandardTestSuitesSources()
                        .flatMapMany { Flux.fromIterable(it) }
                        .flatMap { testSuitesSourceDto ->
                            testSuitesPreprocessorController.fetch(testSuitesSourceDto)
                        }.doOnError {
                            log.error("Error to update standard test suite sources")
                        }
                        .subscribeOn(scheduler)
                        .subscribe()
                }

    /**
     * Execute tests by execution id:
     * - Post request to backend to find all tests by test suite id which are set in execution and create TestExecutions for them
     * - Send a request to orchestrator to initialize agents and start tests execution
     */
    @Suppress(
        "LongParameterList",
        "TOO_MANY_PARAMETERS",
        "UnsafeCallOnNullableType"
    )
    private fun Execution.executeTests(): Mono<StatusResponse> = webClientBackend.post()
        .uri("/executeTestsByExecutionId?executionId=$id")
        .retrieve()
        .toBodilessEntity()
        .then(initializeAgents(this))
        .onErrorResume { ex ->
            val failReason = "Error during preprocessing. Reason: ${ex.message}"
            log.error(
                "$failReason, will mark execution.id=$id as failed; error: ",
                ex
            )
            updateExecutionStatus(id!!, ExecutionStatus.ERROR, failReason)
        }

    private fun getExecution(executionId: Long) = webClientBackend.get()
        .uri("/execution?id=$executionId")
        .retrieve()
        .bodyToMono<Execution>()

    @Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
    private fun updateExecution(
        requestBase: ExecutionRequestBase,
        executionVersion: String,
        testSuiteIds: List<Long>,
    ): Mono<Execution> {
        val executionUpdate = ExecutionInitializationDto(requestBase.project, testSuiteIds, executionVersion, requestBase.execCmd, requestBase.batchSizeForAnalyzer)
        return webClientBackend.makeRequest(BodyInserters.fromValue(executionUpdate), "/updateNewExecution") { spec ->
            spec.onStatus({ status -> status != HttpStatus.OK }) { clientResponse ->
                log.error("Error when making update to execution fro project id = ${requestBase.project.id} ${clientResponse.statusCode()}")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Execution not found"
                )
            }
            spec.bodyToMono()
        }
    }

    @Suppress("MagicNumber")
    private fun cleanupInOrchestrator(executionId: Long) =
            webClientOrchestrator.post()
                .uri("/cleanup?executionId=$executionId")
                .httpRequest {
                    // increased timeout, because orchestrator should finish cleaning up first
                    it.getNativeRequest<HttpClientRequest>()
                        .responseTimeout(Duration.ofSeconds(10))
                }
                .retrieve()
                .toBodilessEntity()

    /**
     * POST request to orchestrator to initiate its work
     */
    private fun initializeAgents(execution: Execution): Status {
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("execution", execution, MediaType.APPLICATION_JSON)
        }

        return webClientOrchestrator
            .post()
            .uri("/initializeAgents")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .toEntity<HttpStatus>()
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
        return toBody(responseSpec)
    }

    private fun updateExecutionStatus(executionId: Long, executionStatus: ExecutionStatus, failReason: String? = null) =
            webClientBackend.makeRequest(
                BodyInserters.fromValue(ExecutionUpdateDto(executionId, executionStatus, failReason)),
                "/updateExecutionByDto"
            ) { it.toEntity<HttpStatus>() }
                .doOnSubscribe {
                    log.info("Making request to set execution status for id=$executionId to $executionStatus")
                }

    @Suppress("NO_BRACES_IN_CONDITIONALS_AND_LOOPS")
    private fun ExecutionRequestBase.getTestSuiteFilter(): (TestSuite) -> Boolean = when (this) {
        is ExecutionRequest -> {
            { true }
        }
        is ExecutionRequestForStandardSuites -> {
            { it.name in this.testSuites }
        }
    }

    private fun ExecutionRequestBase.toAcceptedResponseMono() =
            Mono.just(ResponseEntity(executionResponseBody(executionId), HttpStatus.ACCEPTED))
}

/**
 * @param executionId
 * @return response body for execution submission request
 */
@Suppress("UnsafeCallOnNullableType")
fun executionResponseBody(executionId: Long?): String = "Clone pending, execution id is ${executionId!!}"
