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
    ): Mono<Unit> = fetchTestSuites(
        testSuitesSourceDto = testSuitesSourceDto,
        cloneObject = version,
        cloneAndProcessDirectoryAction = when (mode) {
            TestSuitesSourceFetchMode.BY_BRANCH -> GitPreprocessorService::cloneBranchAndProcessDirectory
            TestSuitesSourceFetchMode.BY_COMMIT -> GitPreprocessorService::cloneCommitAndProcessDirectory
            TestSuitesSourceFetchMode.BY_TAG -> GitPreprocessorService::cloneTagAndProcessDirectory
        }
    )

    private fun fetchTestSuites(
        testSuitesSourceDto: TestSuitesSourceDto,
        cloneObject: String,
        cloneAndProcessDirectoryAction: CloneAndProcessDirectoryAction,
    ): Mono<Unit> = gitPreprocessorService.cloneAndProcessDirectoryAction(testSuitesSourceDto.gitDto, cloneObject) { repositoryDirectory, gitCommitInfo ->
        testsPreprocessorToBackendBridge.doesTestSuitesSourceContainVersion(testSuitesSourceDto, gitCommitInfo.id)
            .filter { doesContain ->
                if (doesContain) {
                    log.info { "Tests source $testSuitesSourceDto already contains snapshot with version ${gitCommitInfo.id}" }
                    false
                } else {
                    true
                }
            }
            .flatMap {
                gitPreprocessorService.archiveToTar(repositoryDirectory / testSuitesSourceDto.testRootPath) { archive ->
                    testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(
                        testSuitesSource = testSuitesSourceDto,
                        version = cloneObject,
                        gitCommitInfo = gitCommitInfo,
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
