package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
import com.saveourtool.save.preprocessor.service.PreprocessorToBackendBridge
import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.utils.detectLatestSha1
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import org.slf4j.Logger
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Preprocessor's controller for [com.saveourtool.save.entities.TestSuitesSource]
 */
@RestController
@RequestMapping("/test-suites-source")
class TestSuitesPreprocessorController(
    private val gitPreprocessorService: GitPreprocessorService,
    private val testDiscoveringService: TestDiscoveringService,
    private val preprocessorToBackendBridge: PreprocessorToBackendBridge,
) {
    /**
     * @param testSuitesSourceDto source of test suites
     * @return list of pulled [TestSuite]s
     */
    @PostMapping("/pull-latest")
    fun pullLatestTestSuites(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
    ): Mono<List<TestSuite>> = detectLatestVersion(testSuitesSourceDto)
        .flatMap { getOrFetchTestSuites(testSuitesSourceDto, it) }

    /**
     * @param testSuitesSourceDto source of test suites
     * @param version which needs to be provided
     * @return list of requested [TestSuite]s
     */
    @PostMapping("/get-or-fetch")
    fun getOrFetchTestSuites(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam version: String,
    ): Mono<List<TestSuite>> =
        preprocessorToBackendBridge.doesTestSuitesSourceContainVersion(testSuitesSourceDto, version)
            .flatMap { contains ->
                if (contains) {
                    getTestSuites(testSuitesSourceDto, version)
                } else {
                    fetchTestSuites(testSuitesSourceDto, version)
                }
            }

    /**
     * @param testSuitesSourceDto source of test suites
     * @param version which needs to be provided
     * @return list of [TestSuite]s associated with provided [version]
     */
    @GetMapping("/get-test-suites")
    fun getTestSuites(
        testSuitesSourceDto: TestSuitesSourceDto,
        version: String,
    ): Mono<List<TestSuite>> = preprocessorToBackendBridge.getTestSuites(testSuitesSourceDto, version)

    /**
     * Detect latest version of TestSuitesSource
     *
     * @param testSuitesSourceDto source of test suites
     * @return latest available version on source
     */
    @GetMapping("/detect-latest-version")
    fun detectLatestVersion(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
    ): Mono<String> = Mono.fromCallable { testSuitesSourceDto.gitDto.detectLatestSha1(testSuitesSourceDto.branch) }

    /**
     * Fetch new tests suites from provided source
     *
     * @param testSuitesSourceDto source from which test suites need to be loaded
     * @param version version which needs to be loaded
     * @return new version of saved snapshot
     */
    @PostMapping("/fetch-version")
    fun fetchTestSuites(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam version: String,
    ): Mono<List<TestSuite>> = fetchTestSuitesFromGit(testSuitesSourceDto, version)

    private fun fetchTestSuitesFromGit(
        testSuitesSourceDto: TestSuitesSourceDto,
        sha1: String,
    ): Mono<List<TestSuite>> {
        return gitPreprocessorService.cloneAndProcessDirectory(testSuitesSourceDto.gitDto, testSuitesSourceDto.branch, sha1) { repositoryDirectory ->
            testDiscoveringService.detectAndSaveAllTestSuitesAndTests(
                repositoryPath = repositoryDirectory,
                testSuitesSourceDto = testSuitesSourceDto,
                version = sha1
            ).flatMap { testSuites ->
                log.info { "Loaded: $testSuites" }
                val content = gitPreprocessorService.archiveToTar(repositoryDirectory)
                preprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(testSuitesSourceDto, sha1, content)
                    .map { testSuites }
            }
        }
    }

    companion object {
        private val log: Logger = getLogger<TestSuitesPreprocessorController>()
    }
}