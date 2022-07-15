package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.ExecutionRequest
import com.saveourtool.save.entities.ExecutionRequestForStandardSuites
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.execution.ExecutionInitializationDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.preprocessor.EmptyResponse
import com.saveourtool.save.preprocessor.StatusResponse
import com.saveourtool.save.preprocessor.TextResponse
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.config.TestSuitesRepo
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.utils.*
import com.saveourtool.save.preprocessor.utils.generateDirectory
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.info

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.codec.multipart.FilePart
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
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.netty.http.client.HttpClientRequest
import reactor.util.function.Tuple2

import java.io.File
import java.io.FileOutputStream
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
    objectMapper: ObjectMapper,
    kotlinSerializationWebClientCustomizer: WebClientCustomizer,
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
    ): Mono<TextResponse> = Mono.just(ResponseEntity(executionResponseBody(executionRequest.executionId), HttpStatus.ACCEPTED))
        .doOnSuccess {
            downLoadRepository(executionRequest)
                .flatMap { (location, version) ->
                    val testRootPath = executionRequest.testRootPath
                    val testRootAbsolutePath = getResourceLocationForGit(location, testRootPath)
                    initializeTestSuitesAndTests(executionRequest.project, testRootPath, testRootAbsolutePath, executionRequest.gitDto.url)
                        .flatMap { testsSuites ->
                            updateExecution(
                                executionRequest.project,
                                location,
                                version,
                                testsSuites.map { it.requiredId() }
                            )
                        }
                }
                .flatMap { it.executeTests() }
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
    ) = Mono.just(ResponseEntity(executionResponseBody(executionRequestForStandardSuites.executionId), HttpStatus.ACCEPTED))
        .doOnSuccess {
            val version = requireNotNull(executionRequestForStandardSuites.version) {
                "Version is not provided for execution.id = ${executionRequestForStandardSuites.executionId}"
            }
            val execCmd = executionRequestForStandardSuites.execCmd
            val batchSizeForAnalyzer = executionRequestForStandardSuites.batchSizeForAnalyzer
            getStandardTestSuiteIds(executionRequestForStandardSuites.testSuites)
                .flatMap { testSuiteIds ->
                    updateExecution(
                        executionRequestForStandardSuites.project,
                        "N/A",
                        version,
                        testSuiteIds,
                        execCmd,
                        batchSizeForAnalyzer,
                    )
                }
                .flatMap { it.executeTests() }
                .subscribeOn(scheduler)
                .subscribe()
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
            val (user, token) = readGitCredentialsForStandardMode(configProperties.reposTokenFileName)
            Flux.fromIterable(readStandardTestSuitesFile(configProperties.reposFileName)).flatMap { testSuiteRepoInfo ->
                val testSuiteUrl = testSuiteRepoInfo.gitUrl
                log.info("Starting clone repository url=$testSuiteUrl for standard test suites")
                val tmpDir = generateDirectory(listOf(testSuiteUrl), configProperties.repository, deleteExisting = false)
                Mono.fromCallable {
                    val gitDto = if (user != null && token != null) {
                        GitDto(url = testSuiteUrl, username = user, password = token)
                    } else {
                        GitDto(testSuiteUrl)
                    }
                    pullOrCloneProjectWithSpecificBranch(gitDto, tmpDir, testSuiteRepoInfo.gitBranchOrCommit)
                        ?.use { /* noop here, just need to close Git object */ }
                }
                    .flatMapMany { Flux.fromIterable(testSuiteRepoInfo.testSuitePaths) }
                    .flatMap { testRootPath ->
                        log.info("Starting to discover root test config in test root path: $testRootPath")
                        val testRootAbsolutePath = tmpDir.resolve(testRootPath).absoluteFile
                        initializeTestSuitesAndTests(null, testRootPath, testRootAbsolutePath, testSuiteUrl)
                    }
                    .doOnError {
                        log.error("Error to update test suite with url=$testSuiteUrl, path=${testSuiteRepoInfo.testSuitePaths}")
                    }
            }
                .flatMapIterable { it }
                .map { it.toDto() }
                .collectList()
                .flatMap {
                    markObsoleteOldStandardTestSuites(it)
                }
                .subscribeOn(scheduler)
                .subscribe()
        }

    private fun markObsoleteOldStandardTestSuites(newTestSuites: List<TestSuiteDto>) = webClientBackend.get()
        .uri("/allStandardTestSuites")
        .retrieve()
        .bodyToMono<List<TestSuiteDto>>()
        .map { existingSuites ->
            existingSuites.filter { it !in newTestSuites }
        }
        .flatMap { obsoleteSuites ->
            webClientBackend.makeRequest(
                BodyInserters.fromValue(obsoleteSuites),
                "/markObsoleteTestSuites"
            ) {
                it.toBodilessEntity()
            }
        }

    private fun downloadRepositoryLocation(gitDto: GitDto): Pair<File, String> {
        val tmpDir = generateDirectory(listOf(gitDto.url), configProperties.repository, deleteExisting = false)
        return tmpDir to tmpDir.relativeTo(File(configProperties.repository)).normalize().path
    }

    @Suppress(
        "TYPE_ALIAS",
        "TOO_LONG_FUNCTION",
        "TOO_MANY_LINES_IN_LAMBDA",
        "UnsafeCallOnNullableType"
    )
    private fun downLoadRepository(executionRequest: ExecutionRequest): Mono<Pair<String, String>> {
        val gitDto = executionRequest.gitDto
        val (tmpDir, location) = downloadRepositoryLocation(gitDto)
        return Mono.fromCallable {
            pullOrCloneProjectWithSpecificBranch(gitDto, tmpDir, branchOrCommit = gitDto.branch ?: gitDto.hash)?.use { git ->
                val version = git.log().call().first()
                    .name
                log.info("Cloned repository ${gitDto.url}, head is at $version")
                return@fromCallable location to version
            }
        }
            .onErrorResume { exception ->
                tmpDir.deleteRecursively()
                val failReason = when (exception) {
                    is InvalidRemoteException,
                    is TransportException,
                    is GitAPIException -> "Error with git API while cloning ${gitDto.url} repository"
                    else -> "Cloning ${gitDto.url} repository failed. Reason: ${exception.message}"
                }
                log.error(failReason, exception)
                updateExecutionStatus(executionRequest.executionId!!, ExecutionStatus.ERROR, failReason).flatMap {
                    Mono.error(exception)
                }
            }
    }

    private fun getStandardTestSuiteIds(testSuiteNames: List<String>): Mono<List<Long>> = webClientBackend.post()
        .uri("/test-suites/standard/ids-by-name")
        .bodyValue(testSuiteNames)
        .retrieve()
        .bodyToMono()

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

    private fun getResourceLocationForGit(location: String, testRootPath: String) = File(configProperties.repository)
        .resolve(location)
        .resolve(testRootPath)

    private fun getExecution(executionId: Long) = webClientBackend.get()
        .uri("${configProperties.backend}/execution?id=$executionId")
        .retrieve()
        .bodyToMono<Execution>()

    @Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
    private fun updateExecution(
        project: Project,
        projectRootRelativePath: String,
        executionVersion: String,
        testSuiteIds: List<Long>,
        execCmd: String? = null,
        batchSizeForAnalyzer: String? = null,
    ): Mono<Execution> {
        val executionUpdate = ExecutionInitializationDto(project, testSuiteIds, projectRootRelativePath, executionVersion, execCmd, batchSizeForAnalyzer)
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

    private fun initializeTestSuitesAndTests(project: Project?,
                                             testRootPath: String,
                                             testRootAbsolutePath: File,
                                             gitUrl: String,
    ): Mono<List<TestSuite>> {
        log.info { "Starting to save new test suites for root test config in $testRootPath" }
        return Mono.fromCallable {
            testDiscoveringService.getRootTestConfig(testRootAbsolutePath.path)
        }
            .zipWhen { rootTestConfig ->
                log.info { "Starting to discover test suites for root test config ${rootTestConfig.location}" }
                discoverAndSaveTestSuites(project, rootTestConfig, testRootPath, gitUrl)
            }
            .flatMap { (rootTestConfig, testSuites) ->
                log.info { "Test suites size = ${testSuites.size}" }
                log.info { "Starting to save new tests for config test root $testRootPath" }
                initializeTests(testSuites, rootTestConfig)
                    .collectList()
                    .map { testSuites }
            }
    }

    private fun discoverAndSaveTestSuites(project: Project?,
                                          rootTestConfig: TestConfig,
                                          testRootPath: String,
                                          gitUrl: String,
    ): Mono<List<TestSuite>> {
        val testSuites: List<TestSuiteDto> = testDiscoveringService.getAllTestSuites(project, rootTestConfig, testRootPath, gitUrl)
        return webClientBackend.makeRequest(BodyInserters.fromValue(testSuites), "/saveTestSuites") {
            it.bodyToMono()
        }
    }

    /**
     * Discover tests and send them to backend
     */
    private fun initializeTests(testSuites: List<TestSuite>,
                                rootTestConfig: TestConfig
    ): Flux<EmptyResponse> = testDiscoveringService.getAllTests(rootTestConfig, testSuites)
        .toFlux()
        .buffer(TESTS_BUFFER_SIZE)
        .doOnNext {
            log.debug { "Processing chuck of tests [${it.first()} ... ${it.last()}]" }
        }
        .flatMap { testDtos ->
            webClientBackend.makeRequest(BodyInserters.fromValue(testDtos), "/initializeTests") {
                it.toBodilessEntity()
            }
        }

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

    @Suppress("TYPE_ALIAS")
    private fun Flux<Tuple2<FilePart, FileInfo>>.download(destination: File): Mono<List<File>> = flatMap { (filePart, fileInfo) ->
        val file = File(destination, filePart.filename()).apply {
            createNewFile()
        }
        // todo: don't use `filename()`
        log.info("Downloading ${filePart.filename()} into ${file.absolutePath}")
        filePart.content().map { dtBuffer ->
            FileOutputStream(file, true).use { os ->
                dtBuffer.asInputStream().use {
                    it.copyTo(os)
                }
            }
            file
        }
            // return a single Mono per file, discarding how many parts `content()` has
            .last()
            .doOnSuccess {
                log.debug("File ${fileInfo.name} should have executable=${fileInfo.isExecutable}")
                if (!it.setExecutable(fileInfo.isExecutable)) {
                    log.warn("Failed to mark file ${fileInfo.name} as executable")
                }
            }
    }
        .collectList()

    private fun updateExecutionStatus(executionId: Long, executionStatus: ExecutionStatus, failReason: String? = null) =
            webClientBackend.makeRequest(
                BodyInserters.fromValue(ExecutionUpdateDto(executionId, executionStatus, failReason)),
                "/updateExecutionByDto"
            ) { it.toEntity<HttpStatus>() }
                .doOnSubscribe {
                    log.info("Making request to set execution status for id=$executionId to $executionStatus")
                }

    companion object {
        // default Webflux in-memory buffer is 256 KiB
        private const val TESTS_BUFFER_SIZE = 128
    }
}

/**
 * @param name file name to read
 * @return map repository to paths to test configs
 */
@Suppress("MagicNumber", "TOO_MANY_LINES_IN_LAMBDA")
fun readStandardTestSuitesFile(name: String) =
        ClassPathResource(name)
            .file
            .readText()
            .lines()
            .filter { it.isNotBlank() }
            .map { line ->
                val splitRow = line.split("\\s".toRegex())
                require(splitRow.size == 3) {
                    "Follow the format for each line: (Gir url) (branch or commit hash) (testRootPath1;testRootPath2;...)"
                }
                TestSuitesRepo(
                    gitUrl = splitRow.first(),
                    gitBranchOrCommit = splitRow[1],
                    testSuitePaths = splitRow[2].split(";")
                )
            }

/**
 * @param executionId
 * @return response body for execution submission request
 */
@Suppress("UnsafeCallOnNullableType")
fun executionResponseBody(executionId: Long?): String = "Clone pending, execution id is ${executionId!!}"

private fun readGitCredentialsForStandardMode(name: String): Pair<String?, String?> {
    val credentialsFile = ClassPathResource(name)
    val fileData = if (credentialsFile.exists()) {
        credentialsFile.file.readLines().single { it.isNotBlank() }
    } else {
        return null to null
    }

    val splitRow = fileData.split("\\s".toRegex())
    require(splitRow.size == 2) {
        "Credentials file should contain git username and git token, separated by whitespace, but provided $splitRow"
    }
    return splitRow.first() to splitRow[1]
}
