package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
import com.saveourtool.save.preprocessor.service.GitRepositoryProcessor
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.service.TestsPreprocessorToBackendBridge
import com.saveourtool.save.request.TestsSourceFetchRequest
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.core.io.FileSystemResource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
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

    private fun fetchTestSuites(
        request: TestsSourceFetchRequest,
        cloneAndProcessDirectoryAction: CloneAndProcessDirectoryAction,
    ): Mono<Unit> = gitPreprocessorService.cloneAndProcessDirectoryAction(
        request.source.gitDto,
        request.version
    ) { repositoryDirectory, gitCommitInfo ->
        val testsSourceSnapshotDto = request.createSnapshot(gitCommitInfo.id, gitCommitInfo.time)
        testsPreprocessorToBackendBridge.doesContainTestsSourceSnapshot(testsSourceSnapshotDto)
            .asyncEffectIf({ this.not() }) {
                doFetchTests(repositoryDirectory, testsSourceSnapshotDto, request.source)
            }
            .flatMap {
                testsPreprocessorToBackendBridge.saveTestsSourceVersion(request.createVersionInfo(gitCommitInfo.id, gitCommitInfo.time))
            }
    }

    private fun doFetchTests(
        repositoryDirectory: Path,
        testsSourceSnapshotDto: TestsSourceSnapshotDto,
        testSuitesSourceDto: TestSuitesSourceDto,
    ): Mono<Unit> = (repositoryDirectory / testSuitesSourceDto.testRootPath).let { pathToRepository ->
        gitPreprocessorService.archiveToTar(pathToRepository) { archive ->
            testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(
                snapshotDto = testsSourceSnapshotDto,
                resourceWithContent = FileSystemResource(archive)
            ).flatMap {
                testDiscoveringService.detectAndSaveAllTestSuitesAndTests(
                    repositoryPath = repositoryDirectory,
                    testSuitesSourceDto = testSuitesSourceDto,
                    version = testsSourceSnapshotDto.commitId
                )
            }
        }
            .map { testSuites ->
                with(testSuitesSourceDto) {
                    log.info { "Loaded ${testSuites.size} test suites from test suites source $name in $organizationName with version ${testsSourceSnapshotDto.commitId}" }
                }
            }
            .doOnError(
                Exception
                ::class.java
            ) { ex ->
                log.error(ex) { "Failed to fetch from ${testsSourceSnapshotDto.commitId}" }
            }
    }

    companion object {
        private val log: Logger = getLogger<TestSuitesPreprocessorController>()
    }
}
