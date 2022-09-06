package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
import com.saveourtool.save.preprocessor.service.GitRepositoryProcessor
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.service.TestsPreprocessorToBackendBridge
import com.saveourtool.save.testsuite.TestSuitesSourceDto
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
     * Fetch new tests suites from provided source from provided tag
     *
     * @param testSuitesSourceDto source from which test suites need to be loaded
     * @param tagName tag which needs to be loaded, will be used as version
     * @return empty response
     */
    @PostMapping("/fetch-from-tag")
    fun fetch(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam tagName: String,
    ): Mono<Unit> = Mono.fromCallable {
        log.debug { "Checking if source ${testSuitesSourceDto.name} needs to be fetched." }
    }
        .filterWhen {
            testsPreprocessorToBackendBridge.doesTestSuitesSourceContainVersion(testSuitesSourceDto, tagName)
                .map(Boolean::not)
        }
        .flatMap { fetchTestSuites(
            GitPreprocessorService::cloneBranchAndProcessDirectory,
            tagName,
            testSuitesSourceDto
        ) }
        .lazyDefaultIfEmpty {
            with(testSuitesSourceDto) {
                log.debug { "Test suites source $name in $organizationName already contains version $tagName" }
            }
        }

    /**
     * Fetch new tests suites from provided source from latest sha-1 in provided branch
     *
     * @param testSuitesSourceDto source from which test suites need to be loaded
     * @param branchName branch from which the latest commit needs to be loaded, will be used as version
     * @return empty response
     */
    @PostMapping("/fetch-from-branch")
    fun fetchFromBranch(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam branchName: String,
    ): Mono<Unit> = fetchTestSuites(
        GitPreprocessorService::cloneBranchAndProcessDirectory,
        branchName,
        testSuitesSourceDto
    ).lazyDefaultIfEmpty {
        with(testSuitesSourceDto) {
            log.debug { "There is no new version for $name in $organizationName" }
        }
    }

    /**
     * Fetch new tests suites from provided source from latest sha-1 in provided branch
     *
     * @param testSuitesSourceDto source from which test suites need to be loaded
     * @param branchName branch from which the latest commit needs to be loaded, will be used as version
     * @return empty response
     */
    @PostMapping("/fetch-from-commit")
    fun fetchFromCommit(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam commitId: String,
    ): Mono<Unit> = fetchTestSuites(
        GitPreprocessorService::cloneCommitAndProcessDirectory,
        commitId,
        testSuitesSourceDto
    ).lazyDefaultIfEmpty {
        with(testSuitesSourceDto) {
            log.debug { "There is no new version for $name in $organizationName" }
        }
    }

    private fun fetchTestSuites(
        cloneAndProcessDirectory: GitPreprocessorService.(GitDto, String, GitRepositoryProcessor<TestSuiteList>) -> Mono<TestSuiteList>,
        cloneObject: String,
        testSuitesSourceDto: TestSuitesSourceDto,
    ): Mono<Unit> = gitPreprocessorService.cloneAndProcessDirectory(testSuitesSourceDto.gitDto, cloneObject) { repositoryDirectory, creationTime ->
        gitPreprocessorService.archiveToTar(repositoryDirectory / testSuitesSourceDto.testRootPath) { archive ->
            testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(
                testSuitesSource = testSuitesSourceDto,
                version = cloneObject,
                creationTime = creationTime,
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
        .log(testSuitesSourceDto, cloneObject)
        .then(Mono.just(Unit))

    private fun Mono<TestSuiteList>.log(testSuitesSourceDto: TestSuitesSourceDto, version: String): Mono<TestSuiteList> = map { testSuites ->
        with(testSuitesSourceDto) {
            log.info { "Loaded ${testSuites.size} test suites from test suites source $name in $organizationName with version $version" }
            testSuites
        }
    }

    companion object {
        private val log: Logger = getLogger<TestSuitesPreprocessorController>()
    }
}
