package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.ExecutionInitializationDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.preprocessor.StatusResponse
import com.saveourtool.save.preprocessor.TextResponse
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.config.TestSuitesRepo
import com.saveourtool.save.preprocessor.service.GithubLocationPreprocessorService
import com.saveourtool.save.preprocessor.service.PreprocessorToBackendBridge
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.utils.*
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.info

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.netty.http.client.HttpClientRequest

import java.io.File
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
    private val githubLocationPreprocessorService: GithubLocationPreprocessorService,
    private val preprocessorToBackendBridge: PreprocessorToBackendBridge
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
    ): Mono<TextResponse> =
            Mono.just(ResponseEntity(executionResponseBody(executionRequest.executionId), HttpStatus.ACCEPTED))
                .doOnSuccess {
                    val gitDto = executionRequest.gitDto
                    val gitLocation = GitLocation(
                        httpUrl = gitDto.url,
                        username = gitDto.username,
                        token = gitDto.password,
                        branch = gitDto.branch ?: detectDefaultBranchName(gitDto.url, gitDto.username, gitDto.password),
                        subDirectory = executionRequest.testRootPath,
                    )
                    val testSuitesSourceDto = TestSuitesSourceDto(
                        type = TestSuiteType.PROJECT,
                        name = gitLocation.defaultTestSuitesSourceName(),
                        description = "Project test from ${gitDto.url}",
                        locationType = TestSuitesSourceLocationType.GIT,
                        locationInfo = gitLocation.formatForDatabase()
                    )
                    getLatestOrInitializeTestSuites(executionRequest.project, testSuitesSourceDto)
                        .flatMap { testsSuites ->
                            updateExecution(
                                project = executionRequest.project,
                                testSuiteIds = testsSuites.map { it.requiredId() },
                            )
                        }
                        .flatMap { it.executeTests() }
                        .subscribeOn(scheduler)
                        .subscribe()
                }

    private fun getLatestOrInitializeTestSuites(
        project: Project?,
        testSuitesSourceDto: TestSuitesSourceDto
    ): Mono<List<TestSuite>> = preprocessorToBackendBridge.findOrCreateTestSuitesSource(testSuitesSourceDto)
        .flatMap { testSuitesSource ->
            require(testSuitesSource.locationType == TestSuitesSourceLocationType.GIT) {
                "Only location type = ${TestSuitesSourceLocationType.GIT} supported now"
            }
            val gitLocation = GitLocation.parseFromDatabase(testSuitesSource.locationInfo)
            val newVersion = gitLocation.detectLatestSha1()
            preprocessorToBackendBridge.checkVersionForTestSuitesSource(testSuitesSource.toDto(), newVersion)
                .doOnNext { exists ->
                    if (exists) {
                        log.debug { "Skip processing for $testSuitesSource: latest version $newVersion already processed" }
                    } else {
                        log.info { "Detected a new version $newVersion for $testSuitesSource" }
                    }
                }
                .filter { it != false }
                .flatMap { preprocessorToBackendBridge.createNewTestSuiteSourceLog(testSuitesSource.toDto(), newVersion) }
                .flatMap { testSuitesSourceLog -> initializeTestSuitesAndTests(project, testSuitesSourceLog) }
                .switchIfEmpty {
                    preprocessorToBackendBridge.findTestSuiteSourceLog(testSuitesSource.toDto(), newVersion).flatMap {
                        preprocessorToBackendBridge.findAllTestSuitesByLog(it)
                    }
                }
        }

    private fun initializeTestSuitesAndTests(project: Project?, testSuitesSourceLog: TestSuitesSourceLog): Mono<List<TestSuite>> =
            githubLocationPreprocessorService.processDirectoryAsMono(testSuitesSourceLog) { repositoryPath ->
                githubLocationPreprocessorService.processTarArchiveAsMono(repositoryPath) { archiveInputStream ->
                    preprocessorToBackendBridge.uploadTestSuitesSourceContent(archiveInputStream, project?.toProjectCoordinates(), testSuitesSourceLog)
                }.flatMap {
                    testDiscoveringService.detectAndSaveAllTestSuitesAndTests(
                        project = project,
                        repositoryPath = repositoryPath,
                        testSuitesSourceLog = testSuitesSourceLog,
                    )
                }
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
            val execCmd = executionRequestForStandardSuites.execCmd
            val batchSizeForAnalyzer = executionRequestForStandardSuites.batchSizeForAnalyzer
            updateExecution(
                executionRequestForStandardSuites.project,
                executionRequestForStandardSuites.testSuiteIds,
                execCmd,
                batchSizeForAnalyzer,
            )
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

    private fun Project.toProjectCoordinates() = ProjectCoordinates(
        organizationName = organization.name,
        projectName = name
    )

    /**
     * Controller to download standard test suites
     *
     * @return Empty response entity
     */
    @Suppress("TOO_LONG_FUNCTION", "TYPE_ALIAS")
    @PostMapping("/uploadStandardTestSuite")
    fun uploadStandardTestSuite() = Mono.just(ResponseEntity("Upload standard test suites pending...\n", HttpStatus.ACCEPTED))
        .doOnSuccess {
            val (username, token) = readGitCredentialsForStandardMode(configProperties.reposTokenFileName)
            Flux.fromIterable(readStandardTestSuitesFile(configProperties.reposFileName)).flatMap { testSuiteRepoInfo ->
                val testSuiteUrl = testSuiteRepoInfo.gitUrl
                Flux.fromIterable(testSuiteRepoInfo.testSuitePaths).flatMap { testSuitePath ->
                    val gitLocation = GitLocation(
                        httpUrl = testSuiteUrl,
                        username = username,
                        token = token,
                        branch = testSuiteRepoInfo.gitBranchOrCommit,
                        subDirectory = testSuitePath,
                    )
                    val testSuitesSourceDto = TestSuitesSourceDto(
                        type = TestSuiteType.STANDARD,
                        name = gitLocation.defaultTestSuitesSourceName(),
                        description = "Standard test from $testSuiteUrl",
                        locationType = TestSuitesSourceLocationType.GIT,
                        locationInfo = gitLocation.formatForDatabase()
                    )

                    getLatestOrInitializeTestSuites(null, testSuitesSourceDto)
                        .doOnError {
                            log.error("Error to update test suite with url=$testSuiteUrl, path=${testSuiteRepoInfo.testSuitePaths}")
                        }
                }
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
        testSuiteIds: List<Long>,
        execCmd: String? = null,
        batchSizeForAnalyzer: String? = null,
    ): Mono<Execution> {
        val executionUpdate = ExecutionInitializationDto(project, testSuiteIds, execCmd, batchSizeForAnalyzer)
        return webClientBackend.makePost(BodyInserters.fromValue(executionUpdate), "/updateNewExecution")
            .bodyToMono()
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

    private fun updateExecutionStatus(executionId: Long, executionStatus: ExecutionStatus, failReason: String? = null) =
            webClientBackend.makePost(
                BodyInserters.fromValue(ExecutionUpdateDto(executionId, executionStatus, failReason)),
                "/updateExecutionByDto"
            ).toEntity<HttpStatus>()
                .doOnSubscribe {
                    log.info("Making request to set execution status for id=$executionId to $executionStatus")
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
