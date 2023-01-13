package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
import com.saveourtool.save.preprocessor.service.GitRepositoryProcessor
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.service.TestsPreprocessorToBackendBridge
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.core.io.FileSystemResource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import kotlin.io.path.div

typealias TestSuiteList = List<TestSuite>
typealias CloneAndProcessDirectoryAction = GitPreprocessorService.(GitDto, String, GitRepositoryProcessor<TestSuiteList>) -> Mono<TestSuiteList>

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
     * @param testSuitesSourceDto source from which test suites need to be loaded
     * @param mode mode of fetching, it controls how [version] is used
     * @param version tag, branch or commit (depends on [mode]) which needs to be loaded, will be used as version
     * @return empty response
     */
    @PostMapping("/fetch")
    fun fetch(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam mode: TestSuitesSourceFetchMode,
        @RequestParam version: String,
    ): Mono<Unit> = when (mode) {
        TestSuitesSourceFetchMode.BY_BRANCH -> fetchFromBranch(testSuitesSourceDto, version)
        TestSuitesSourceFetchMode.BY_COMMIT -> fetchFromCommit(testSuitesSourceDto, version)
        TestSuitesSourceFetchMode.BY_TAG -> fetchFromTag(testSuitesSourceDto, version)
        TestSuitesSourceFetchMode.UNKNOWN -> error("Not expected mode $mode")
    }

    /**
     * Fetch new tests suites from provided source from provided tag
     *
     * @param testSuitesSourceDto source from which test suites need to be loaded
     * @param tagName tag which needs to be loaded, will be used as version
     * @return empty response
     */
    private fun fetchFromTag(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam tagName: String,
    ): Mono<Unit> = fetchTestSuites(
        testSuitesSourceDto,
        tagName,
        GitPreprocessorService::cloneTagAndProcessDirectory
    )

    /**
     * Fetch new tests suites from provided source from latest sha-1 in provided branch
     *
     * @param testSuitesSourceDto source from which test suites need to be loaded
     * @param branchName branch from which the latest commit needs to be loaded, will be used as version
     * @return empty response
     */
    private fun fetchFromBranch(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam branchName: String,
    ): Mono<Unit> = fetchTestSuites(
        testSuitesSourceDto,
        branchName,
        GitPreprocessorService::cloneBranchAndProcessDirectory
    )

    /**
     * Fetch new tests suites from provided source from commitId
     *
     * @param testSuitesSourceDto source from which test suites need to be loaded
     * @param commitId commit which needs to be loaded, will be used as version
     * @return empty response
     */
    private fun fetchFromCommit(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam commitId: String,
    ): Mono<Unit> = fetchTestSuites(
        testSuitesSourceDto,
        commitId,
        GitPreprocessorService::cloneCommitAndProcessDirectory
    )

    private fun fetchTestSuites(
        testSuitesSourceDto: TestSuitesSourceDto,
        cloneObject: String,
        cloneAndProcessDirectoryAction: CloneAndProcessDirectoryAction,
    ): Mono<Unit> = gitPreprocessorService.cloneAndProcessDirectoryAction(testSuitesSourceDto.gitDto, cloneObject) { repositoryDirectory, commitInfo ->
        gitPreprocessorService.archiveToTar(repositoryDirectory / testSuitesSourceDto.testRootPath) { archive ->
            testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(
                testSuitesSource = testSuitesSourceDto,
                version = cloneObject,
                creationTime = commitInfo.time,
                resourceWithContent = FileSystemResource(archive)
            ).flatMap {
                testDiscoveringService.detectAndSaveAllTestSuitesAndTests(
                    repositoryPath = repositoryDirectory,
                    testSuitesSourceDto = testSuitesSourceDto,
                    version = cloneObject
                )
            }
        }
    }
        .map { testSuites ->
            with(testSuitesSourceDto) {
                log.info { "Loaded ${testSuites.size} test suites from test suites source $name in $organizationName with version $cloneObject" }
            }
        }
        .doOnError(Exception::class.java) { ex ->
            log.error(ex) { "Failed to fetch from $cloneObject" }
        }
        .onErrorReturn(Unit)

    companion object {
        private val log: Logger = getLogger<TestSuitesPreprocessorController>()
    }
}
