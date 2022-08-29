package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
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
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.div

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
        .flatMap { fetchTestSuitesFromTag(testSuitesSourceDto, tagName) }
        .log(testSuitesSourceDto, tagName)
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
    ): Mono<Unit> = fetchTestSuitesFromBranch(testSuitesSourceDto, branchName)
        .log(testSuitesSourceDto, branchName)
        .lazyDefaultIfEmpty {
            with(testSuitesSourceDto) {
                log.debug { "There is no new version for $name in $organizationName" }
            }
        }

    private fun fetchTestSuitesFromTag(
        testSuitesSourceDto: TestSuitesSourceDto,
        tagName: String,
    ): Mono<List<TestSuite>> = gitPreprocessorService.cloneTagAndProcessDirectory(
        testSuitesSourceDto.gitDto,
        tagName
    ) { repositoryDirectory, creationTime ->
        fetchTestSuites(testSuitesSourceDto, tagName, repositoryDirectory, creationTime)
    }

    private fun fetchTestSuitesFromBranch(
        testSuitesSourceDto: TestSuitesSourceDto,
        branchName: String,
    ): Mono<List<TestSuite>> = gitPreprocessorService.cloneBranchAndProcessDirectory(
        testSuitesSourceDto.gitDto,
        branchName
    ) { repositoryDirectory, creationTime ->
        fetchTestSuites(testSuitesSourceDto, branchName, repositoryDirectory, creationTime)
    }

    private fun fetchTestSuites(
        testSuitesSourceDto: TestSuitesSourceDto,
        version: String,
        repositoryDirectory: Path,
        creationTime: Instant,
    ): Mono<List<TestSuite>> =
            gitPreprocessorService.archiveToTar(repositoryDirectory / testSuitesSourceDto.testRootPath) { archive ->
                testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(
                    testSuitesSource = testSuitesSourceDto,
                    version = version,
                    creationTime = creationTime,
                    resourceWithContent = FileSystemResource(archive)
                ).flatMap {
                    testDiscoveringService.detectAndSaveAllTestSuitesAndTests(
                        repositoryPath = repositoryDirectory,
                        testSuitesSourceDto = testSuitesSourceDto,
                        version = version
                    )
                }
            }

    private fun Mono<List<TestSuite>>.log(testSuitesSourceDto: TestSuitesSourceDto, version: String): Mono<Unit> = map {
        with(testSuitesSourceDto) {
            log.info { "Loaded ${it.size} test suites from test suites source $name in $organizationName with version $version" }
        }
    }

    companion object {
        private val log: Logger = getLogger<TestSuitesPreprocessorController>()
    }
}
