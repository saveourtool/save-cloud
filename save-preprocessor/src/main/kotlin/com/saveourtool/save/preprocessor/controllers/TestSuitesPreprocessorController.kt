package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
import com.saveourtool.save.preprocessor.service.GitRepositoryProcessor
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.service.TestsPreprocessorToBackendBridge
import com.saveourtool.save.preprocessor.utils.GitCommitInfo
import com.saveourtool.save.request.TestsSourceFetchRequest
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.save.utils.*

import org.jetbrains.annotations.NonBlocking
import org.slf4j.Logger
import org.springframework.core.io.FileSystemResource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.nio.file.Path

import kotlin.io.path.div

typealias CloneAndProcessDirectoryAction = GitPreprocessorService.(GitDto, String, GitRepositoryProcessor<Unit>) -> Mono<Unit>

/**
 * Preprocessor's controller for [com.saveourtool.save.entities.TestSuitesSource]
 */
@RestController
@RequestMapping("/test-suites-sources")
class TestSuitesPreprocessorController(
    private val gitPreprocessorService: GitPreprocessorService,
    private val testDiscoveringService: TestDiscoveringService,
    private val testsPreprocessorToBackendBridge: TestsPreprocessorToBackendBridge,
) {
    /**
     * Fetch new tests suites from provided source from provided version
     *
     * @param request request to fetch tests from tests source
     * @return empty response
     */
    @PostMapping("/fetch")
    fun fetch(
        @RequestBody request: TestsSourceFetchRequest,
    ): Mono<Unit> = fetchTestSuites(
        request = request,
        cloneAndProcessDirectoryAction = when (request.mode) {
            TestSuitesSourceFetchMode.BY_BRANCH -> GitPreprocessorService::cloneBranchAndProcessDirectory
            TestSuitesSourceFetchMode.BY_COMMIT -> GitPreprocessorService::cloneCommitAndProcessDirectory
            TestSuitesSourceFetchMode.BY_TAG -> GitPreprocessorService::cloneTagAndProcessDirectory
        }
    )

    @NonBlocking
    private fun fetchTestSuites(
        request: TestsSourceFetchRequest,
        cloneAndProcessDirectoryAction: CloneAndProcessDirectoryAction,
    ): Mono<Unit> = gitPreprocessorService.cloneAndProcessDirectoryAction(
        request.source.gitDto,
        request.version
    ) { repositoryDirectory, gitCommitInfo ->
        testsPreprocessorToBackendBridge.findTestsSourceSnapshot(request.source.requiredId(), gitCommitInfo.id)
            .switchIfEmpty {
                doFetchTests(repositoryDirectory, gitCommitInfo, request)
            }
            .flatMap { snapshot ->
                testsPreprocessorToBackendBridge.saveTestsSourceVersion(request.createVersion(snapshot))
            }.doOnNext { isSaved: Boolean ->
                log.info {
                    val messagePrefix = "Tests from ${request.source.gitDto.url}"
                    val status = when {
                        isSaved -> "saved"
                        else -> "not saved: the snapshot already exists"
                    }
                    val messageSuffix = "(version \"${request.version}\"; commit ${gitCommitInfo.id})."

                    "$messagePrefix $status $messageSuffix"
                }
            }.thenReturn(Unit)
    }

    @NonBlocking
    private fun doFetchTests(
        repositoryDirectory: Path,
        gitCommitInfo: GitCommitInfo,
        request: TestsSourceFetchRequest,
    ): Mono<TestsSourceSnapshotDto> = (repositoryDirectory / request.source.testRootPath).let { pathToRepository ->
        gitPreprocessorService.archiveToTar(pathToRepository) { archive ->
            testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(
                snapshotDto = request.createSnapshot(gitCommitInfo.id, gitCommitInfo.time),
                resourceWithContent = FileSystemResource(archive)
            ).zipWhen { snapshot ->
                testDiscoveringService.detectAndSaveAllTestSuitesAndTests(
                    repositoryPath = repositoryDirectory,
                    testRootPath = request.source.testRootPath,
                    sourceSnapshot = snapshot,
                )
            }
        }
            .map { (snapshot, testSuites) ->
                log.info { "Loaded ${testSuites.size} test suites from tests snapshot id ${snapshot.requiredId()} with version ${snapshot.commitId}" }
                snapshot
            }
            .doOnError(
                Exception
                ::class.java
            ) { ex ->
                log.error(ex) { "Failed to fetch from ${gitCommitInfo.id}" }
            }
    }

    companion object {
        private val log: Logger = getLogger<TestSuitesPreprocessorController>()
    }
}
