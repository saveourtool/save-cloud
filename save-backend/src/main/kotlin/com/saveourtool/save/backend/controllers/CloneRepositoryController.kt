package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.blockingToMono
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.*
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.orNotFound
import com.saveourtool.save.v1

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller to save project
 */
@RestController
@RequestMapping("/api")
class CloneRepositoryController(
    private val testSuitesService: TestSuitesService,
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage,
    private val fileStorage: FileStorage,
    private val gitService: GitService,
    private val runExecutionController: RunExecutionController,
) {
    /**
     * Endpoint to save project
     *
     * @param executionRequest information about project
     * @param files resources for execution
     * @param authentication [Authentication] representing an authenticated request
     * @return mono string
     */
    @PostMapping(path = ["/$v1/submitExecutionRequest"], consumes = ["multipart/form-data"])
    fun submitExecutionRequest(
        @RequestPart(required = true) executionRequest: ExecutionRequest,
        @RequestPart("file", required = false) files: Flux<ShortFileInfo>,
        authentication: Authentication,
    ): Mono<StringResponse> =
            sendToTrigger(
                executionRequest,
                authentication,
                files,
                { true }
            ) {
                val branch = with(executionRequest.branchOrCommit) {
                    if (this?.startsWith("origin/") == true) {
                        replaceFirst("origin/", "")
                    } else {
                        throw IllegalArgumentException("Branch should be specified")
                    }
                }
                val testSuitesSource = testSuitesSourceService.getOrCreate(
                    executionRequest.project.organization,
                    gitService.findByOrganizationAndUrl(executionRequest.project.organization, executionRequest.gitDto.url)
                        .orNotFound(),
                    executionRequest.testRootPath,
                    branch,
                )
                listOf(testSuitesSource)
            }

    /**
     * Endpoint to save project as binary file
     *
     * @param executionRequestForStandardSuites information about project
     * @param files files required for execution
     * @param authentication [Authentication] representing an authenticated request
     * @return mono string
     */
    @PostMapping(path = ["/$v1/executionRequestStandardTests"], consumes = ["multipart/form-data"])
    fun executionRequestStandardTests(
        @RequestPart("execution", required = true) executionRequestForStandardSuites: ExecutionRequestForStandardSuites,
        @RequestPart("file", required = true) files: Flux<ShortFileInfo>,
        authentication: Authentication,
    ): Mono<StringResponse> =
            sendToTrigger(
                executionRequestForStandardSuites,
                authentication,
                files,
                { it.name in executionRequestForStandardSuites.testSuites }
            ) {
                testSuitesSourceService.getStandardTestSuitesSources()
            }

    @Suppress("TOO_LONG_FUNCTION")
    private fun <T : ExecutionRequestBase> sendToTrigger(
        executionRequest: T,
        authentication: Authentication,
        shortFiles: Flux<ShortFileInfo>,
        testSuitesFilter: (TestSuite) -> Boolean,
        testSuitesSourceResolver: (T) -> List<TestSuitesSource>,
    ): Mono<StringResponse> {
        val projectCoordinates = with(executionRequest.project) {
            ProjectCoordinates(organization.name, name)
        }
        val testSuiteIdsMono = blockingToMono { testSuitesSourceResolver(executionRequest) }
            .flatMapIterable { it }
            .map { testSuitesSource ->
                testSuitesSourceSnapshotStorage.list(testSuitesSource.organization.name, testSuitesSource.name)
                    .collectList()
                    .map { keys ->
                        keys.maxByOrNull(TestSuitesSourceSnapshotKey::creationTimeInMills)?.version
                            ?: throw IllegalStateException("Failed to detect latest version for $testSuitesSource")
                    }
                    .flatMapIterable { testSuitesService.getBySourceAndVersion(testSuitesSource, it) }
                    .filter(testSuitesFilter)
                    .map { it.requiredId() }
            }
            .let {
                Flux.concat(it)
            }
            .collectList()
        return fileStorage.convertToLatestFileInfo(projectCoordinates, shortFiles)
            .collectList()
            .zipWith(testSuiteIdsMono)
            .map { (files, testSuitesIds) ->
                ExecutionRunRequest(
                    projectCoordinates = projectCoordinates,
                    testSuiteIds = testSuitesIds,
                    files = files.map { it.toStorageKey() },
                    sdk = executionRequest.sdk,
                    execCmd = executionRequest.execCmd,
                    batchSizeForAnalyzer = executionRequest.batchSizeForAnalyzer,
                )
            }
            .flatMap {
                runExecutionController.trigger(it, authentication)
            }
            .map {
                ResponseEntity.accepted()
                    .body("Clone pending, execution id is $it")
            }
    }
}
