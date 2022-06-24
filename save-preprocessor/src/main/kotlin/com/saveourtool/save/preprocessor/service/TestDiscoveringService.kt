package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.ConfigDetector
import com.saveourtool.save.core.files.createRelativePathToTheRoot
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.utils.buildActivePlugins
import com.saveourtool.save.core.utils.processInPlace
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.preprocessor.utils.toHash
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteType

import okio.FileSystem
import okio.Path.Companion.toPath
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * A service that call SAVE core to discover test suites and tests
 */
@Service
class TestDiscoveringService {
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
     * Discover all test suites in the project
     *
     * @param project a [Project] corresponding to analyzed data. If it null - standard test suites
     * @param testRootPath path to the test repository root where save.properties and high level save.toml can be stored
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @param testSuiteRepoUrl url of the repo with test suites
     * @return a list of [TestSuiteDto]s
     * @throws IllegalArgumentException when provided path doesn't point to a valid config file
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getAllTestSuites(
        project: Project?,
        rootTestConfig: TestConfig,
        testRootPath: String,
        testSuiteRepoUrl: String,
    ) = rootTestConfig
        .getAllTestConfigs()
        .asSequence()
        .mapNotNull { it.getGeneralConfigOrNull() }
        .filterNot { it.suiteName == null }
        .filterNot { it.description == null }
        .map { config ->
            // we operate here with suite names from only those TestConfigs, that have General section with suiteName key
            TestSuiteDto(
                project?.let { TestSuiteType.PROJECT } ?: TestSuiteType.STANDARD,
                config.suiteName!!,
                config.description,
                project,
                testRootPath,
                testRootPath.removeSuffix(rootTestConfig.directory.createRelativePathToTheRoot(rootTestConfig.getRootConfig().directory).toString()),
                testSuiteRepoUrl,
                config.language,
            )
        }
        .distinct()
        .toList()

    /**
     * Discover all tests in the project
     *
     * @param testSuites testSuites in this project
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @return a list of [TestDto]s
     * @throws PluginException if configs use unknown plugin
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getAllTests(rootTestConfig: TestConfig, testSuites: List<TestSuite>) = rootTestConfig
        .getAllTestConfigs()
        .asSequence()
        .flatMap { testConfig ->
            val plugins = testConfig.buildActivePlugins(emptyList())
            val generalConfig = testConfig.getGeneralConfigOrNull()
            if (plugins.isEmpty() || generalConfig == null) {
                return@flatMap emptySequence()
            }
            val testSuite = testSuites.firstOrNull { it.name == generalConfig.suiteName }
            requireNotNull(testSuite) {
                "No test suite matching name=${generalConfig.suiteName} is provided. Provided names are: ${testSuites.map { it.name }}"
            }
            plugins.asSequence().flatMap { plugin ->
                plugin.discoverTestFiles(testConfig.directory)
                    .map {
                        val testRelativePath = it.test.toFile()
                            .relativeTo(rootTestConfig.getRootConfig().directory.toFile())
                            .path
                        TestDto(testRelativePath, plugin::class.simpleName!!, testSuite.id!!, it.test.toFile().toHash(), generalConfig.tags!!)
                    }
            }
        }
        .onEach {
            log.debug("Discovered the following test: $it")
        }

    private fun TestConfig.getGeneralConfigOrNull() = pluginConfigs.filterIsInstance<GeneralConfig>().singleOrNull()

    companion object {
        private val log = LoggerFactory.getLogger(TestDiscoveringService::class.java)
    }
}
