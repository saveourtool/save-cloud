package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.defaultConfig
import org.cqfn.save.domain.FileInfo
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
import org.cqfn.save.preprocessor.config.TestSuitesRepo
import org.cqfn.save.preprocessor.service.TestDiscoveringService
import org.cqfn.save.preprocessor.utils.*
import org.cqfn.save.preprocessor.utils.generateDirectory
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType
import org.cqfn.save.utils.moveFileWithAttributes

import com.fasterxml.jackson.databind.ObjectMapper
import okio.FileSystem
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
import org.springframework.web.bind.annotation.RequestParam
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
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.netty.http.client.HttpClientRequest
import reactor.util.function.Tuple2

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
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
    private val webClientBackend = WebClient.builder().baseUrl(configProperties.backend)
        .apply(kotlinSerializationWebClientCustomizer::customize)
        .build()
    private val webClientOrchestrator = WebClient.builder().baseUrl(configProperties.orchestrator).codecs {
        it.defaultCodecs().multipartCodecs().encoder(Jackson2JsonEncoder(objectMapper))
    }
        .apply(kotlinSerializationWebClientCustomizer::customize)
        .build()
    private val scheduler = Schedulers.boundedElastic()

    /**
     * @param executionRequest Dto of repo information to clone and project info
     * @param files resources required for execution
     * @param fileInfos a list of [FileInfo]s associated with [files]
     * @return response entity with text
     */
    @Suppress("TOO_LONG_FUNCTION")
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart(required = true) executionRequest: ExecutionRequest,
        @RequestPart("fileInfo", required = false) fileInfos: Flux<FileInfo>,
        @RequestPart("file", required = false) files: Flux<FilePart>,
    ): Mono<TextResponse> = Mono.just(ResponseEntity("Clone pending", HttpStatus.ACCEPTED))
        .doOnSuccess {
            downLoadRepository(executionRequest)
                .flatMap { (location, version) ->
                    val resourcesLocation = getResourceLocationForGit(location, executionRequest.testRootPath)
                    log.info("Downloading additional files into $resourcesLocation")
                    files.zipWith(fileInfos).download(resourcesLocation)
                        .switchIfEmpty(
                            // if no files have been provided, proceed with empty list
                            Mono.just(emptyList())
                        )
                        .map {
                            log.info("Downloaded ${it.size} files into $resourcesLocation")
                            Pair(location, version)
                        }
                }
                .flatMap { (location, version) ->
                    updateExecution(executionRequest.project, location, version).map { execution ->
                        Pair(execution, location)
                    }
                }
                .flatMap { (execution, location) ->
                    sendToBackendAndOrchestrator(
                        execution,
                        executionRequest.project,
                        executionRequest.testRootPath,
                        location,
                        null,
                        executionRequest.gitDto.url
                    )
                }
                .subscribeOn(scheduler)
                .subscribe()
        }

    /**
     * @param executionRequestForStandardSuites Dto of binary file, test suites names and project info
     * @param files resources for execution
     * @param fileInfos a list of [FileInfo]s associated with [files]
     * @return response entity with text
     */
    @PostMapping(value = ["/uploadBin"], consumes = ["multipart/form-data"])
    fun uploadBin(
        @RequestPart executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        @RequestPart("file", required = true) files: Flux<FilePart>,
        @RequestPart("fileInfo", required = true) fileInfos: Flux<FileInfo>,
    ) = Mono.just(ResponseEntity("Clone pending", HttpStatus.ACCEPTED))
        .doOnSuccess { _ ->
            files.zipWith(fileInfos).download(File(FileSystem.SYSTEM_TEMPORARY_DIRECTORY.toString()))
                .flatMap { files ->
                    saveBinaryFile(executionRequestForStandardSuites, files)
                }
                .subscribeOn(scheduler)
                .subscribe()
        }

    /**
     * Accept execution rerun request
     *
     * @param executionRerunRequest request
     * @param executionType
     * @return status 202
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    @PostMapping("/rerunExecution")
    fun rerunExecution(@RequestBody executionRerunRequest: ExecutionRequest, @RequestParam executionType: ExecutionType) = Mono.fromCallable {
        requireNotNull(executionRerunRequest.executionId) { "Can't rerun execution with unknown id" }
        ResponseEntity("Clone pending", HttpStatus.ACCEPTED)
    }
        .doOnSuccess {
            updateExecutionStatus(executionRerunRequest.executionId!!, ExecutionStatus.PENDING)
                .flatMap {
                    cleanupInOrchestrator(executionRerunRequest.executionId!!)
                }
                .flatMap {
                    getExecutionLocation(executionRerunRequest, executionType)
                }
                .flatMap { location ->
                    getExecution(executionRerunRequest.executionId!!).map { location to it }
                }
                .flatMap { (location, execution) ->
                    getTestSuitesIfStandard(executionType, execution, location)
                }
                .flatMap { (location, execution, testSuites) ->
                    val files = execution.additionalFiles?.split(";")?.filter { it.isNotBlank() }?.map { File(it) } ?: emptyList()
                    val resourcesLocation = getResourceLocation(executionType, location, executionRerunRequest.testRootPath, files)

                    files.forEach { file ->
                        log.debug("Copy additional file $file into ${resourcesLocation.resolve(file.name)}")
                        Files.copy(
                            Paths.get(file.absolutePath),
                            Paths.get(resourcesLocation.resolve(file.name).absolutePath),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES
                        )
                        // FixMe: currently it's quite rough solution, to make all additional files executable
                        // FixMe: https://github.com/analysis-dev/save-cloud/issues/442
                        if (!resourcesLocation.resolve(file.name).setExecutable(true)) {
                            log.warn("Failed to mark file ${resourcesLocation.resolve(file.name)} as executable")
                        }
                    }
                    sendToBackendAndOrchestrator(
                        execution,
                        execution.project,
                        executionRerunRequest.testRootPath,
                        location,
                        testSuites?.map { it.toDto() },
                        executionRerunRequest.gitDto.url,
                    )
                }
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
            val newTestSuites: MutableList<TestSuiteDto> = mutableListOf()
            Flux.fromIterable(readStandardTestSuitesFile(configProperties.reposFileName)).flatMap { testSuiteRepoInfo ->
                val testSuiteUrl = testSuiteRepoInfo.gitUrl
                log.info("Starting clone repository url=$testSuiteUrl for standard test suites")
                val tmpDir = generateDirectory(listOf(testSuiteUrl), configProperties.repository, deleteExisting = false)
                Mono.fromCallable {
                    if (user != null && token != null) {
                        pullOrCloneProjectWithSpecificBranch(GitDto(url = testSuiteUrl, username = user, password = token), tmpDir, testSuiteRepoInfo.gitBranchOrCommit)
                    } else {
                        pullOrCloneProjectWithSpecificBranch(GitDto(testSuiteUrl), tmpDir, testSuiteRepoInfo.gitBranchOrCommit)
                    }
                        ?.use { /* noop here, just need to close Git object */ }
                }
                    .flatMapMany { Flux.fromIterable(testSuiteRepoInfo.testSuitePaths) }
                    .flatMap { testRootPath ->
                        log.info("Starting to discover root test config in test root path: $testRootPath")
                        val testResourcesRootAbsolutePath = tmpDir.resolve(testRootPath).absolutePath
                        val rootTestConfig = testDiscoveringService.getRootTestConfig(testResourcesRootAbsolutePath)
                        log.info("Starting to discover standard test suites for root test config ${rootTestConfig.location}")
                        val testRootRelativePath = rootTestConfig.directory.toFile().relativeTo(tmpDir).toString()
                        val testSuiteDtos = testDiscoveringService.getAllTestSuites(null, rootTestConfig, testRootRelativePath, testSuiteUrl)
                        testSuiteDtos.forEach { newTestSuites.add(it) }
                        log.info("Test suites size = ${testSuiteDtos.size}")
                        log.info("Starting to save new test suites for root test config in $testRootPath")
                        webClientBackend.makeRequest(BodyInserters.fromValue(testSuiteDtos), "/saveTestSuites") {
                            it.bodyToMono<List<TestSuite>>()
                        }
                            .flatMap { testSuites ->
                                log.info("Starting to save new tests for config test root $testRootPath")
                                initializeTests(
                                    testSuites,
                                    rootTestConfig,
                                    null,
                                )
                            }
                    }
                    .doOnError {
                        log.error("Error to update test suite with url=$testSuiteUrl, path=${testSuiteRepoInfo.testSuitePaths}")
                    }
            }
                .collectList()
                .flatMap {
                    markObsoleteOldStandardTestSuites(newTestSuites)
                }
                .subscribeOn(scheduler)
                .subscribe()
        }

    private fun markObsoleteOldStandardTestSuites(newTestSuites: MutableList<TestSuiteDto>) = webClientBackend.get()
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

    @Suppress(
        "TYPE_ALIAS",
        "TOO_LONG_FUNCTION",
        "TOO_MANY_LINES_IN_LAMBDA",
        "UnsafeCallOnNullableType"
    )
    private fun downLoadRepository(executionRequest: ExecutionRequest): Mono<Pair<String, String>> {
        val gitDto = executionRequest.gitDto
        val tmpDir = generateDirectory(listOf(gitDto.url), configProperties.repository, deleteExisting = false)
        return Mono.fromCallable {
            pullOrCloneProjectWithSpecificBranch(gitDto, tmpDir, branchOrCommit = gitDto.branch ?: gitDto.hash)?.use { git ->
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
                updateExecutionStatus(executionRequest.executionId!!, ExecutionStatus.ERROR).flatMap {
                    Mono.error(exception)
                }
            }
    }

    @Suppress("TOO_LONG_FUNCTION")
    private fun saveBinaryFile(
        executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        files: List<File>,
    ): Mono<StatusResponse> {
        // Move files into local storage
        val tmpDir = generateDirectory(calculateTmpNameForFiles(files), configProperties.repository)
        files.forEach {
            log.debug("Move $it into $tmpDir")
            moveFileWithAttributes(it, tmpDir)
        }
        val project = executionRequestForStandardSuites.project
        // TODO: Save the proper version https://github.com/analysis-dev/save-cloud/issues/321
        val version = files.first().name
        val execCmd = executionRequestForStandardSuites.execCmd
        val batchSizeForAnalyzer = executionRequestForStandardSuites.batchSizeForAnalyzer
        return updateExecution(
            executionRequestForStandardSuites.project,
            tmpDir.name,
            version,
            executionRequestForStandardSuites.testsSuites.joinToString(),
            execCmd,
            batchSizeForAnalyzer,
        )
            .flatMap { execution ->
                sendToBackendAndOrchestrator(
                    execution,
                    project,
                    // stub for standard tests that won't be used
                    "N/A",
                    tmpDir.relativeTo(File(configProperties.repository)).normalize().path,
                    executionRequestForStandardSuites.testsSuites.map {
                        TestSuiteDto(
                            TestSuiteType.STANDARD,
                            it,
                            null,
                            project,
                            // stub for standard tests that won't be used
                            "N/A"
                        )
                    }
                )
            }
    }

    /**
     * Note: `testSuiteDtos != null` only if execution type is STANDARD
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
        testRootPath: String,
        projectRootRelativePath: String,
        testSuiteDtos: List<TestSuiteDto>?,
        gitUrl: String? = null,
    ): Mono<StatusResponse> {
        val executionType = execution.type
        testSuiteDtos?.let {
            require(executionType == ExecutionType.STANDARD) { "Test suites shouldn't be provided unless ExecutionType is STANDARD (actual: $executionType)" }
        } ?: require(executionType == ExecutionType.GIT) { "Test suites are not provided, but should for executionType=$executionType" }

        return if (executionType == ExecutionType.GIT) {
            prepareForExecutionFromGit(project, execution, testRootPath, projectRootRelativePath, gitUrl!!)
        } else {
            prepareExecutionForStandard(testSuiteDtos!!, execution)
        }
            .then(initializeAgents(execution, testSuiteDtos))
            .onErrorResume { ex ->
                log.error(
                    "Error during preprocessing, will mark execution.id=${execution.id} as failed; error: ",
                    ex
                )
                updateExecutionStatus(execution.id!!, ExecutionStatus.ERROR)
            }
    }

    private fun getResourceLocation(
        executionType: ExecutionType,
        location: String,
        testRootPath: String,
        files: List<File>,
    ) = if (executionType == ExecutionType.GIT) {
        getResourceLocationForGit(location, testRootPath)
    } else {
        getTmpDirName(calculateTmpNameForFiles(files), "${configProperties.repository}")
    }

    private fun getResourceLocationForGit(location: String, testRootPath: String) = File(configProperties.repository)
        .resolve(location)
        .resolve(testRootPath)

    private fun calculateTmpNameForFiles(files: List<File>) = files.map { it.toHash() }.sorted()

    private fun getExecutionLocation(executionRerunRequest: ExecutionRequest, executionType: ExecutionType) = if (executionType == ExecutionType.GIT) {
        downLoadRepository(executionRerunRequest).map { (location, _) -> location }
    } else {
        // In standard mode we will calculate location later, according list of additional files
        Mono.just("")
    }

    private fun getExecution(executionId: Long) = webClientBackend.get()
        .uri("${configProperties.backend}/execution?id=$executionId")
        .retrieve()
        .bodyToMono<Execution>()

    @Suppress("UnsafeCallOnNullableType")
    private fun getTestSuitesIfStandard(executionType: ExecutionType, execution: Execution, location: String) = if (executionType == ExecutionType.GIT) {
        // Do nothing
        Mono.fromCallable { Triple(location, execution, null) }
    } else {
        getTestSuitesById(execution.testSuiteIds!!).map { Triple(location, execution, it) }
    }

    private fun getTestSuitesById(testSuiteIds: String) = testSuiteIds.split(", ").let {
        Flux.fromIterable(it).flatMap {
            webClientBackend.get()
                .uri("/testSuite/$it")
                .retrieve()
                .bodyToMono<TestSuite>()
        }
            .collectList()
    }

    @Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
    private fun updateExecution(
        project: Project,
        projectRootRelativePath: String,
        executionVersion: String,
        testSuiteIds: String = "ALL",
        execCmd: String? = null,
        batchSizeForAnalyzer: String? = null,
    ): Mono<Execution> {
        val executionUpdate = ExecutionInitializationDto(project, testSuiteIds, projectRootRelativePath, executionVersion, execCmd, batchSizeForAnalyzer)
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

    @Suppress("UnsafeCallOnNullableType")
    private fun prepareForExecutionFromGit(project: Project,
                                           execution: Execution,
                                           testRootPath: String,
                                           projectRootRelativePath: String,
                                           gitUrl: String,
    ): Mono<List<EmptyResponse>> = Mono.fromCallable {
        val testResourcesRootAbsolutePath =
                getTestResourcesRootAbsolutePath(testRootPath, projectRootRelativePath)
        testDiscoveringService.getRootTestConfig(testResourcesRootAbsolutePath)
    }
        .zipWhen { rootTestConfig ->
            discoverAndSaveTestSuites(project, rootTestConfig, testRootPath, gitUrl)
        }
        .flatMap { (rootTestConfig, testSuites) ->
            val testSuiteIds = testSuites.map { it.id!! }.sorted()
            execution.testSuiteIds = testSuiteIds.joinToString()
            updateExecution(execution).map { rootTestConfig to testSuites }
        }
        .flatMap { (rootTestConfig, testSuites) ->
            initializeTests(testSuites, rootTestConfig, execution.id!!)
        }

    @Suppress("TYPE_ALIAS", "UnsafeCallOnNullableType")
    private fun prepareExecutionForStandard(
        testSuiteDtos: List<TestSuiteDto>,
        execution: Execution
    ): Mono<ResponseEntity<HttpStatus>> {
        val testSuiteIds: MutableList<Long> = mutableListOf()
        return Flux.fromIterable(testSuiteDtos).flatMap<List<TestSuite>?> {
            webClientBackend.get()
                .uri("/standardTestSuitesWithName?name=${it.name}")
                .retrieve()
                .bodyToMono()
        }.flatMap { testSuites ->
            Flux.fromIterable(testSuites).flatMap { testSuite ->
                testSuiteIds.add(testSuite.id!!)
                webClientBackend.makeRequest(
                    BodyInserters.fromValue(execution.id!!),
                    "/saveTestExecutionsForStandardByTestSuiteId?testSuiteId=${testSuite.id}"
                ) {
                    it.toBodilessEntity()
                }
            }
        }
            .collectList()
            .flatMap {
                testSuiteIds.sort()
                execution.testSuiteIds = testSuiteIds.joinToString()
                updateExecution(execution)
            }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun getTestResourcesRootAbsolutePath(testRootPath: String,
                                                 projectRootRelativePath: String): String {
        // TODO: File should be provided without explicit naming of `save.propeties`
        val propertiesFile = File(configProperties.repository, projectRootRelativePath)
            .resolve("$testRootPath/save.properties")
        val saveProperties: SaveProperties = if (propertiesFile.exists()) {
            decodeFromPropertiesFile<SaveProperties>(propertiesFile)
                .mergeConfigWithPriorityToThis(defaultConfig())
        } else {
            defaultConfig()
        }
        return propertiesFile.parentFile
            .resolve(saveProperties.testFiles!!.firstOrNull() ?: ".")
            .absolutePath
    }

    private fun discoverAndSaveTestSuites(project: Project,
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
    @Suppress("MagicNumber")
    private fun initializeTests(testSuites: List<TestSuite>,
                                rootTestConfig: TestConfig,
                                executionId: Long?,
    ): Mono<List<EmptyResponse>> = testDiscoveringService.getAllTests(
        rootTestConfig,
        testSuites,
    )
        // default Webflux in-memory buffer is 256 KiB
        .chunked(128)
        .toFlux()
        .doOnNext {
            log.debug("Processing chuck of tests [${it.first()} ... ${it.last()}]")
        }
        .flatMap { testDtos ->
            webClientBackend.makeRequest(
                BodyInserters.fromValue(testDtos),
                "/initializeTests${(executionId?.let { "?executionId=$it" } ?: "")}"
            ) { it.toBodilessEntity() }
        }
        .collectList()

    /**
     * POST request to orchestrator to initiate its work
     */
    private fun initializeAgents(
        execution: Execution,
        testSuiteDtos: List<TestSuiteDto>?
    ): Status {
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("execution", execution, MediaType.APPLICATION_JSON)
        }

        testSuiteDtos?.let {
            bodyBuilder.part("testSuiteDtos", testSuiteDtos, MediaType.APPLICATION_JSON)
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

    private fun updateExecutionStatus(executionId: Long, executionStatus: ExecutionStatus) =
            webClientBackend.makeRequest(
                BodyInserters.fromValue(ExecutionUpdateDto(executionId, executionStatus)),
                "/updateExecutionByDto"
            ) { it.toEntity<HttpStatus>() }
                .doOnSubscribe {
                    log.info("Making request to set execution status for id=$executionId to $executionStatus")
                }

    @Suppress("UnsafeCallOnNullableType")
    private fun updateExecution(execution: Execution) =
            webClientBackend.makeRequest(
                BodyInserters.fromValue(execution),
                "/updateExecution"
            ) { it.toEntity<HttpStatus>() }
                .doOnSubscribe {
                    log.info("Making request to update execution with id=${execution.id!!}")
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
            .map {
                val splitRow = it.split("\\s".toRegex())
                require(splitRow.size == 3) {
                    "Follow the format for each line: (Gir url) (branch or commit hash) (testRootPath1;testRootPath2;...)"
                }
                TestSuitesRepo(
                    gitUrl = splitRow.first(),
                    gitBranchOrCommit = splitRow[1],
                    testSuitePaths = splitRow[2].split(";")
                )
            }

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
