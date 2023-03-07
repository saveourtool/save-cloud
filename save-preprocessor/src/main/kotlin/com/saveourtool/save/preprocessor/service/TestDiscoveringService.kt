@file:Suppress("FILE_UNORDERED_IMPORTS")

package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.config.TestConfigSections
import com.saveourtool.save.core.files.ConfigDetector
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.core.plugin.PluginException
import com.saveourtool.save.core.utils.buildActivePlugins
import com.saveourtool.save.core.utils.processInPlace
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugins.fix.FixPlugin
import com.saveourtool.save.plugins.fixandwarn.FixAndWarnPluginConfig
import com.saveourtool.save.preprocessor.utils.toHash
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.test.collectPluginNames
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.EmptyResponse
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.error
import com.saveourtool.save.utils.info
import com.saveourtool.save.utils.requireIsAbsolute
import com.saveourtool.save.utils.thenJust
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.util.regex.PatternSyntaxException
import kotlin.io.path.absolute
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.relativeToOrNull
import java.nio.file.Path as NioPath

/**
 * A service that call SAVE core to discover test suites and tests
 */
@Service
class TestDiscoveringService(
    private val testsPreprocessorToBackendBridge: TestsPreprocessorToBackendBridge,
    private val validationService: TestSuiteValidationService,
) {
    /**
     * @param repositoryPath
     * @param testRootPath
     * @param sourceSnapshot
     * @return list of [TestSuite] initialized from provided directory
     */
    fun detectAndSaveAllTestSuitesAndTests(
        repositoryPath: NioPath,
        testRootPath: String,
        sourceSnapshot: TestsSourceSnapshotDto,
    ): Mono<List<TestSuite>> {
        log.info { "Starting to save new test suites for root test config in $repositoryPath" }
        val rootTestConfigAsync = blockingToMono {
            getRootTestConfig((repositoryPath / testRootPath).absolute().normalize())
        }

        return rootTestConfigAsync
            .zipWhen { rootTestConfig ->
                {
                    log.info { "Starting to discover test suites for root test config ${rootTestConfig.location}" }
                    val testSuites: List<TestSuiteDto> = discoverAllTestSuites(
                        rootTestConfig,
                        sourceSnapshot,
                    )
                    testSuites.forEach { testSuite ->
                        log.info { "XXX " }
                    }
                    testSuites
                }.toMono()
            }
            .flatMap { (rootTestConfig, testSuites) ->
                blockingToMono {
                    log.info { "Test suites size = ${testSuites.size}" }
                    log.info { "Starting to save new tests for config test root $repositoryPath" }
                    discoverAllTestsIntoMap(rootTestConfig, testSuites)
                }
            }
            .saveTestSuitesAndTests()
    }

    /**
     * Returns a root config of hierarchy; this config will already have all descendants merged with their parents.
     *
     * @param testResourcesRootAbsolutePath path to directory with root config
     * @return a root [TestConfig]
     * @throws IllegalArgumentException in case of invalid testConfig file
     */
    @Blocking
    fun getRootTestConfig(testResourcesRootAbsolutePath: NioPath): TestConfig =
            ConfigDetector(FileSystem.SYSTEM).configFromFile(testResourcesRootAbsolutePath.requireIsAbsolute().toOkioPath()).apply {
                getAllTestConfigs().onEach {
                    it.processInPlace()
                }
            }

    /**
     * Discover all test suites in the test suites source
     *
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @param sourceSnapshot tests snapshot
     * @return a list of [TestSuiteDto]s
     * @throws IllegalArgumentException when provided path doesn't point to a valid config file
     */
    @NonBlocking
    @Suppress(
        "UnsafeCallOnNullableType",
        "LongMethod",
        "MagicNumber",
        "RedundantHigherOrderMapUsage",
        "VARIABLE_NAME_INCORRECT",
        "TOO_LONG_FUNCTION",
        "WRONG_NEWLINES",
    )
    fun getAllTestSuites(
        rootTestConfig: TestConfig,
        sourceSnapshot: TestsSourceSnapshotDto,
    ): List<TestSuiteDto> {
        log.info { "XXX getAllTestSuites()" }
        val t0 = System.nanoTime()
        val allTestConfigs = rootTestConfig
            .getAllTestConfigs()
        val t1 = System.nanoTime()
        @Suppress("FLOAT_IN_ACCURATE_CALCULATIONS")
        log.info { "XXX getAllTestConfigs() took ${(t1 - t0) / 1000L / 1e3} ms" }

        return allTestConfigs
            .asSequence()
            .map { testConfig: TestConfig -> // XXX Replace with onEach or forEach?
                log.info { "XXX Test config: $testConfig" }
                val pluginConfigs = testConfig.pluginConfigs
                    .asSequence()
                    .filterNot { config ->
                        config is GeneralConfig
                    }

                @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
                val errors = mutableListOf<String>()

                @Suppress("TYPE_ALIAS")
                val resources: MutableMap<TestConfigSections, MutableList<NioPath>> = pluginConfigs.fold(mutableMapOf()) { acc, config ->
                    acc.apply {
                        compute(config.type) { _, valueOrNull: MutableList<NioPath>? ->
                            val resourceNames = config.getResourceNames().getOrElse { error ->
                                errors += error
                                emptySequence()
                            }

                            (valueOrNull ?: mutableListOf()).apply {
                                addAll(resourceNames)
                            }
                        }
                    }
                }

                resources.forEach { type, resourceNames ->
                    log.info { "\t$type" }
                    resourceNames.forEach {
                        log.info { "\t\t$it" }
                    }
                }
                errors.forEach { error ->
                    log.info { "\t$error" }
                }

                val warnConfigs = pluginConfigs.asSequence().mapNotNull { config ->
                    when (config) {
                        is WarnPluginConfig -> config
                        is FixAndWarnPluginConfig -> config.warn
                        else -> null
                    }
                }.toList()
                log.info { "XXX Warn configs: ${warnConfigs.size}" }
                warnConfigs.forEachIndexed { index, config ->
                    log.info { "\t$index: wildCardInDirectoryMode = ${config.wildCardInDirectoryMode}" }  // XXX Should be null
                }

                testConfig
            }
            .mapNotNull { it.getGeneralConfigOrNull() }
            .filterNot { it.suiteName == null }
            .filterNot { it.description == null }
            .map { generalConfig: GeneralConfig ->
                log.info { "XXX General config: $generalConfig" }
                generalConfig
            }
            .map { config ->
                // we operate here with suite names from only those TestConfigs, that have General section with suiteName key
                TestSuiteDto(
                    config.suiteName!!,
                    config.description,
                    sourceSnapshot,
                    config.language,
                    config.tags
                )
            }
            .distinct()
            .toList()
    }

    @Suppress("TYPE_ALIAS", "WRONG_NEWLINES")
    private fun PluginConfig.getResourceNames(): Either<String, Sequence<NioPath>> {
        val resourceNamePattern = try {
            Regex(resourceNamePatternStr)
        } catch (_: PatternSyntaxException) {
            return "Resource name pattern is not a valid regular expression: \"$resourceNamePatternStr\"".left()
        }

        val configLocation = configLocation.toNioPath()
        if (!configLocation.isRegularFile()) {
            return "Config file doesn't exist: \"$configLocation\"".left()
        }

        val testDirectory = configLocation.parent
            ?: return "The parent directory of the config file is null: \"$configLocation\"".left()
        if (!testDirectory.isDirectory()) {
            return "Test directory doesn't exist: \"$testDirectory\"".left()
        }

        return testDirectory.listDirectoryEntries().asSequence()
            .filter(NioPath::isRegularFile)
            .filterNot { file ->
                file.name.equals("save.toml", ignoreCase = true)
            }
            .filterNot { file ->
                file.name.equals("save.properties", ignoreCase = true)
            }
            .map { file ->
                file.relativeToOrNull(testDirectory)
            }
            .filterNotNull()
            .filterNot(NioPath::isAbsolute)
            .filter { relativeFile ->
                relativeFile.name.matches(resourceNamePattern)
            }
            .right()
    }

    /**
     * Discover all test suites in the project
     *
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @param sourceSnapshot source snapshot with test suites
     * @return a list of saved [TestSuite]s
     * @throws IllegalArgumentException when provided path doesn't point to a valid config file
     */
    @NonBlocking
    @Suppress("UnsafeCallOnNullableType")
    fun discoverAllTestSuites(
        rootTestConfig: TestConfig,
        sourceSnapshot: TestsSourceSnapshotDto,
    ): List<TestSuiteDto> =
            getAllTestSuites(rootTestConfig, sourceSnapshot)

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
    @Blocking
    @Suppress("UnsafeCallOnNullableType", "TOO_MANY_LINES_IN_LAMBDA")
    fun getAllTests(rootTestConfig: TestConfig, testSuites: List<TestSuiteDto>) = rootTestConfig
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
                        val (allFiles, additionalFiles) = if (it is FixPlugin.FixTestFiles) {
                            listOf(it.test, it.expected) to
                                    listOf(it.expected.getRelativePath(rootTestConfig))
                        } else {
                            listOf(it.test) to emptyList()
                        }
                        val testRelativePath = it.test.getRelativePath(rootTestConfig)
                        testSuite to TestDto(
                            testRelativePath,
                            plugin::class.simpleName!!,
                            0,
                            allFiles.toHash(),
                            additionalFiles,
                        )
                    }
            }
        }
        .onEach { (testSuite, tests) ->
            log.debug("For test suite ${testSuite.name} discovered the following tests: $tests")
        }

    /**
     * Discover all tests in the project
     *
     * @param testSuites testSuites in this project
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @return a list of [TestDto]s
     * @throws PluginException if configs use unknown plugin
     */
    @Blocking
    @Suppress("UnsafeCallOnNullableType")
    fun discoverAllTestsIntoMap(
        rootTestConfig: TestConfig,
        testSuites: List<TestSuiteDto>,
    ) = getAllTests(rootTestConfig, testSuites).convertToMap().updatePluginNames()

    @NonBlocking
    @Suppress("TYPE_ALIAS")
    private fun Mono<Map<TestSuiteDto, List<TestDto>>>.saveTestSuitesAndTests() = flatMap {
        it.saveTestSuites()
    }.map { testsMap ->
        testsMap.mapValues { (testSuite, tests) ->
            tests.map { it.copy(testSuiteId = testSuite.requiredId()) }
        }
    }.flatMap { testsMaps ->
        testsMaps.values
            .flatten()
            .toFlux()
            .save()
            .thenJust(testsMaps.keys.toList())
    }

    @NonBlocking
    @Suppress("TYPE_ALIAS")
    private fun Map<TestSuiteDto, List<TestDto>>.saveTestSuites() = entries
        .toFlux()
        .flatMap { (testSuiteDto, tests) ->
            testSuiteDto.save().map { it to tests }
        }
        .collectList()
        .map { it.toMap() }

    @Suppress("TYPE_ALIAS")
    private fun Map<TestSuiteDto, List<TestDto>>.saveTests() =
            values.flatten()
                .toFlux()
                .save()

    @Suppress("TYPE_ALIAS")
    private fun Sequence<Pair<TestSuiteDto, TestDto>>.convertToMap() = groupBy({ (testSuite, _) ->
        testSuite
    }) { (_, test) ->
        test
    }

    @Suppress("TYPE_ALIAS")
    private fun Map<TestSuiteDto, List<TestDto>>.updatePluginNames() = map { (testSuite, tests) ->
        val collectedPluginNames = tests.collectPluginNames()
        log.debug {
            "Test suite ${testSuite.name} has [$collectedPluginNames] plugins."
        }
        testSuite.copy(plugins = collectedPluginNames) to tests
    }
        .toMap()

    private fun TestConfig.getGeneralConfigOrNull() = pluginConfigs.filterIsInstance<GeneralConfig>().singleOrNull()

    /**
     * Save test suites via backend
     */
    @NonBlocking
    private fun TestSuiteDto.save(): Mono<TestSuite> = testsPreprocessorToBackendBridge.saveTestSuite(this)

    /**
     * Save tests via backend
     */
    @NonBlocking
    private fun Flux<TestDto>.save(): Flux<EmptyResponse> = testsPreprocessorToBackendBridge.saveTests(this)

    companion object {
        private val log = LoggerFactory.getLogger(TestDiscoveringService::class.java)
    }
}
