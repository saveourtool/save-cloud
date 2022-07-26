package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.ConfigDetector
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.utils.buildActivePlugins
import com.saveourtool.save.core.utils.processInPlace
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.plugins.fix.FixPlugin
import com.saveourtool.save.preprocessor.EmptyResponse
import com.saveourtool.save.preprocessor.utils.toHash
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.utils.info
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import kotlin.io.path.absolutePathString

/**
 * A service that call SAVE core to discover test suites and tests
 */
@Service
class TestDiscoveringService(
    private val testsPreprocessorToBackendBridge: TestsPreprocessorToBackendBridge,
) {
    /**
     * @param repositoryPath
     * @param testSuitesSourceDto
     * @param version
     * @return list of [TestSuite] initialized from provided directory
     */
    fun detectAndSaveAllTestSuitesAndTests(
        repositoryPath: java.nio.file.Path,
        testSuitesSourceDto: TestSuitesSourceDto,
        version: String,
    ): Mono<List<TestSuite>> {
        log.info { "Starting to save new test suites for root test config in $repositoryPath" }
        return Mono.fromCallable { getRootTestConfig(repositoryPath.absolutePathString()) }
            .zipWhen { rootTestConfig ->
                log.info { "Starting to discover test suites for root test config ${rootTestConfig.location}" }
                discoverAndSaveAllTestSuites(
                    rootTestConfig,
                    testSuitesSourceDto,
                    version
                )
            }
            .flatMap { (rootTestConfig, testSuites) ->
                log.info { "Test suites size = ${testSuites.size}" }
                log.info { "Starting to save new tests for config test root $repositoryPath" }
                discoverAndSaveAllTests(rootTestConfig, testSuites)
                    .collectList()
                    .map { testSuites }
            }
    }

    /**
     * Returns a root config of hierarchy; this config will already have all descendants merged with their parents.
     *
     * @param testResourcesRootAbsolutePath path to directory with root config
     * @return a root [TestConfig]
     * @throws IllegalArgumentException in case of invalid testConfig file
     */
    fun getRootTestConfig(testResourcesRootAbsolutePath: String): TestConfig =
            ConfigDetector(FileSystem.SYSTEM).configFromFile(testResourcesRootAbsolutePath.toPath()).apply {
                getAllTestConfigs().onEach {
                    it.processInPlace()
                }
            }

    /**
     * Discover all test suites in the test suites source
     *
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @param source
     * @param version
     * @return a list of [TestSuiteDto]s
     * @throws IllegalArgumentException when provided path doesn't point to a valid config file
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getAllTestSuites(
        rootTestConfig: TestConfig,
        source: TestSuitesSourceDto,
        version: String,
    ) = rootTestConfig
        .getAllTestConfigs()
        .asSequence()
        .mapNotNull { it.getGeneralConfigOrNull() }
        .filterNot { it.suiteName == null }
        .filterNot { it.description == null }
        .map { config ->
            // we operate here with suite names from only those TestConfigs, that have General section with suiteName key
            TestSuiteDto(
                config.suiteName!!,
                config.description,
                source,
                version,
                config.language,
                config.tags
            )
        }
        .distinct()
        .toList()

    /**
     * Discover all test suites in the project
     *
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @param source source with test suites
     * @param version
     * @return a list of saved [TestSuite]s
     * @throws IllegalArgumentException when provided path doesn't point to a valid config file
     */
    @Suppress("UnsafeCallOnNullableType")
    fun discoverAndSaveAllTestSuites(
        rootTestConfig: TestConfig,
        source: TestSuitesSourceDto,
        version: String,
    ) = getAllTestSuites(rootTestConfig, source, version).save()

    private fun Path.getRelativePath(rootTestConfig: TestConfig) = this.toFile()
        .relativeTo(rootTestConfig.directory.toFile())
        .path

    /**
     * Discover all tests in the project
     *
     * @param testSuites testSuites in this project
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @return a list of [TestDto]s
     * @throws PluginException if configs use unknown plugin
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_MANY_LINES_IN_LAMBDA")
    fun getAllTests(rootTestConfig: TestConfig, testSuites: List<TestSuite>) = rootTestConfig
        .getAllTestConfigs()
        .asSequence()
        .flatMap { testConfig ->
            val plugins = testConfig.buildActivePlugins(emptyList())
            val generalConfig = testConfig.getGeneralConfigOrNull()
            if (plugins.isEmpty() || generalConfig == null) {
                return@flatMap emptySequence()
            }
            val testSuite = requireNotNull(testSuites.firstOrNull { it.name == generalConfig.suiteName }) {
                "No test suite matching name=${generalConfig.suiteName} is provided. Provided names are: ${testSuites.map { it.name }}"
            }
            plugins.asSequence().flatMap { plugin ->
                plugin.discoverTestFiles(testConfig.directory)
                    .map {
                        val additionalFiles = if (it is FixPlugin.FixTestFiles) {
                            listOf(it.expected.getRelativePath(rootTestConfig))
                        } else {
                            emptyList()
                        }
                        val testRelativePath = it.test.getRelativePath(rootTestConfig)
                        TestDto(
                            testRelativePath,
                            plugin::class.simpleName!!,
                            testSuite.id!!,
                            it.test.toFile().toHash(),
                            additionalFiles,
                        )
                    }
            }
        }
        .onEach {
            log.debug("Discovered the following test: $it")
        }

    /**
     * Discover all tests in the project
     *
     * @param testSuites testSuites in this project
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @return a list of [TestDto]s
     * @throws PluginException if configs use unknown plugin
     */
    @Suppress("UnsafeCallOnNullableType")
    fun discoverAndSaveAllTests(rootTestConfig: TestConfig, testSuites: List<TestSuite>) = getAllTests(rootTestConfig, testSuites)
        .toFlux()
        .save()

    private fun getTestLinesByPath(pathPrefix: String, testPath: String?) = testPath?.let {
        (pathPrefix.toPath() / it).toFile().readLines()
    }

    /**
     * @param testFilesRequest request for test files
     * @return [TestFilesContent] of public tests with additional info
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getPublicTestFiles(testFilesRequest: TestFilesRequest) = TestFilesContent(
        getTestLinesByPath(testFilesRequest.testRootPath, testFilesRequest.test.filePath)!!,
        getTestLinesByPath(testFilesRequest.testRootPath, testFilesRequest.test.additionalFiles.firstOrNull()),
    )

    private fun TestConfig.getGeneralConfigOrNull() = pluginConfigs.filterIsInstance<GeneralConfig>().singleOrNull()

    /**
     * Save test suites via backend
     */
    private fun List<TestSuiteDto>.save(): Mono<List<TestSuite>> = testsPreprocessorToBackendBridge.saveTestSuites(this)

    /**
     * Save tests via backend
     */
    private fun Flux<TestDto>.save(): Flux<EmptyResponse> = testsPreprocessorToBackendBridge.saveTests(this)

    companion object {
        private val log = LoggerFactory.getLogger(TestDiscoveringService::class.java)
    }
}
