package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.execution.ExecutionInitializationDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.preprocessor.StatusResponse
import com.saveourtool.save.preprocessor.TextResponse
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.config.TestSuitesRepo
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.utils.*
import com.saveourtool.save.utils.info

import com.fasterxml.jackson.databind.ObjectMapper
import com.saveourtool.save.entities.*
import com.saveourtool.save.preprocessor.service.PreprocessorToBackendBridge
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
    private val testDiscoveringService: TestDiscoveringService,
    private val objectMapper: ObjectMapper,
    kotlinSerializationWebClientCustomizer: WebClientCustomizer,
    private val testSuitesPreprocessorController: TestSuitesPreprocessorController,
    private val preprocessorToBackendBridge: PreprocessorToBackendBridge,
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
    private val standardTestSuitesRepo: TestSuitesRepo by lazy {
        ClassPathResource(configProperties.reposFileName)
            .file
            .let {
                objectMapper.readValue(it, TestSuitesRepo::class.java)!!
            }
    }

    /**
     * @param executionRequest Dto of repo information to clone and project info
     * @return response entity with text
     */
    @Suppress("TOO_LONG_FUNCTION")
    @PostMapping("/upload")
    fun upload(
        @RequestBody executionRequest: ExecutionRequest,
    ): Mono<TextResponse> = executionResponseAsMono(executionRequest)
        .doOnSuccess {
            upload(
                executionRequest.project.organization.name,
                executionRequest.gitDto.url,
                executionRequest.testRootPath,
                executionRequest.branchOrCommit
                    ?.takeIf { it.startsWith("origin/") }
                    ?.replaceFirst("origin/", "")
                    ?: executionRequest.gitDto.detectDefaultBranchName(),
                { true },
                executionRequest.project,
                null,
                null
            )
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
    ) = executionResponseAsMono(executionRequestForStandardSuites)
        .doOnSuccess {
            Flux.fromIterable(standardTestSuitesRepo.testRootPaths)
                .flatMap { testRootPath ->
                    upload(
                        standardTestSuitesRepo.organizationName,
                        standardTestSuitesRepo.url,
                        testRootPath,
                        standardTestSuitesRepo.branch,
                        { it.name in executionRequestForStandardSuites.testSuites },
                        executionRequestForStandardSuites.project,
                        executionRequestForStandardSuites.execCmd,
                        executionRequestForStandardSuites.batchSizeForAnalyzer
                    )
                }
                .subscribeOn(scheduler)
                .subscribe()
        }

    private fun upload(
        organizationName: String,
        gitUrl: String,
        testRootPath: String,
        branch: String,
        testSuiteFilter: (TestSuite) -> Boolean,
        project: Project,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
    ): Mono<StatusResponse> = preprocessorToBackendBridge.getTestSuitesSource(
        organizationName,
        gitUrl,
        testRootPath,
        branch
    ).flatMap { testSuitesSource ->
        preprocessorToBackendBridge.getTestSuitesLatestVersion(organizationName, testSuitesSource.name)
            .flatMap { version ->
                preprocessorToBackendBridge.getTestSuites(
                    organizationName,
                    testSuitesSource.name,
                    version
                )
                    .flatMapMany { Flux.fromIterable(it) }
                    .filter(testSuiteFilter)
                    .map { it.requiredId() }
                    .collectList()
                    .flatMap { testSuiteIds ->
                        updateExecution(
                            project,
                            version,
                            testSuiteIds,
                            execCmd,
                            batchSizeForAnalyzer,
                        )
                    }
                    .flatMap { it.executeTests() }
            }
    }

    /**
     * Accept execution rerun request
     *
     * @param executionRerunRequest request
     * @return status 202
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    @PostMapping("/rerunExecution")
    fun rerunExecution(@RequestBody executionRerunRequest: ExecutionRequest) = Mono.fromCallable {
        requireNotNull(executionRerunRequest.executionId) { "Can't rerun execution with unknown id" }
        ResponseEntity("Clone pending", HttpStatus.ACCEPTED)
    }
        .doOnSuccess {
            updateExecutionStatus(executionRerunRequest.executionId!!, ExecutionStatus.PENDING)
                .flatMap { cleanupInOrchestrator(executionRerunRequest.executionId!!) }
                .flatMap { getExecution(executionRerunRequest.executionId!!) }
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
    fun uploadStandardTestSuite() = Mono.just(ResponseEntity("Upload standard test suites pending...\n", HttpStatus.ACCEPTED))
        .doOnSuccess {
            Flux.fromIterable(standardTestSuitesRepo.testRootPaths)
                .flatMap { testRootPath ->
                    val testSuitesSourceAsMono = preprocessorToBackendBridge.getTestSuitesSource(
                        organizationName = standardTestSuitesRepo.organizationName,
                        gitUrl = standardTestSuitesRepo.url,
                        testRootPath = testRootPath,
                        branch = standardTestSuitesRepo.branch
                    )
                    testSuitesSourceAsMono
                        .map { it.toDto() }
                        .flatMap { testSuitesSourceDto ->
                            testSuitesPreprocessorController.pullLatestTestSuites(testSuitesSourceDto)
                        }.doOnError {
                            log.error("Error to update test suite with url=${standardTestSuitesRepo.url}, path=${testRootPath}")
                        }
                }
                .doOnNext {
                    log.info("Loaded: $it")
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
        .uri("${configProperties.backend}/execution?id=$executionId")
        .retrieve()
        .bodyToMono<Execution>()

    @Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
    private fun updateExecution(
        project: Project,
        executionVersion: String,
        testSuiteIds: List<Long>,
        execCmd: String? = null,
        batchSizeForAnalyzer: String? = null,
    ): Mono<Execution> {
        val executionUpdate = ExecutionInitializationDto(project, testSuiteIds, executionVersion, execCmd, batchSizeForAnalyzer)
        return webClientBackend.makeRequest(BodyInserters.fromValue(executionUpdate), "/updateNewExecution") { spec ->
            spec.onStatus({ status -> status != HttpStatus.OK }) { clientResponse ->
                log.error("Error when making update to execution fro project id = ${project.id} ${clientResponse.statusCode()}")
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
}

/**
 * @param executionId
 * @return response body for execution submission request
 */
@Suppress("UnsafeCallOnNullableType")
fun executionResponseBody(executionId: Long?): String = "Clone pending, execution id is ${executionId!!}"

private fun executionResponseAsMono(executionRequest: ExecutionRequestBase) =
        Mono.just(ResponseEntity(executionResponseBody(executionRequest.executionId), HttpStatus.ACCEPTED))
