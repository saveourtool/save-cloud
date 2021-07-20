package org.cqfn.save.preprocessor.service

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.utils.buildActivePlugins
import org.cqfn.save.core.utils.processInPlace
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.preprocessor.utils.toHash
import org.cqfn.save.test.TestDto
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType

import okio.ExperimentalFileSystem
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
    @OptIn(ExperimentalFileSystem::class)
    fun getRootTestConfig(testResourcesRootAbsolutePath: String): TestConfig =
            ConfigDetector().configFromFile(testResourcesRootAbsolutePath.toPath()).apply {
                getAllTestConfigs().onEach {
                    it.processInPlace()
                }
            }

    /**
     * Discover all test suites in the project
     *
     * @param project a [Project] corresponding to analyzed data
     * @param propertiesRelativePath path to save.properties file relative to repository root
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @return a list of [TestSuiteDto]s
     * @throws IllegalArgumentException when provided path doesn't point to a valid config file
     */
    fun getAllTestSuites(project: Project, rootTestConfig: TestConfig, propertiesRelativePath: String) = rootTestConfig
        .getAllTestConfigs()
        .mapNotNull { it.getGeneralConfigOrNull()?.suiteName }
        .map { suiteName ->
            // we operate here with suite names from only those TestConfigs, that have General section with suiteName key
            TestSuiteDto(TestSuiteType.PROJECT, suiteName, project, propertiesRelativePath)
        }
        .distinct()

    /**
     * Discover all tests in the project
     *
     * @param testSuites testSuites in this project
     * @param rootTestConfig root config of SAVE configs hierarchy
     * @return a list of [TestDto]s
     * @throws PluginException if configs use unknown plugin
     */
    @OptIn(ExperimentalFileSystem::class)
    @Suppress("UnsafeCallOnNullableType")
    fun getAllTests(rootTestConfig: TestConfig, testSuites: List<TestSuite>) = rootTestConfig
        .getAllTestConfigs()
        .flatMap { testConfig ->
            val plugins = testConfig.buildActivePlugins(emptyList())
            val generalConfig = testConfig.getGeneralConfigOrNull()
            if (plugins.isEmpty() || generalConfig == null) {
                return@flatMap emptyList()
            }
            val testSuite = testSuites.firstOrNull { it.name == generalConfig.suiteName }
            requireNotNull(testSuite) {
                "No test suite matching name=${generalConfig.suiteName} is provided. Provided names are: ${testSuites.map { it.name }}"
            }
            plugins.flatMap { plugin ->
                plugin.discoverTestFiles(testConfig.directory)
                    .map {
                        val testRelativePath = it.first().toFile()
                            .relativeTo(rootTestConfig.directory.toFile())
                            .path
                        TestDto(testRelativePath, plugin::class.simpleName!!, testSuite.id!!, it.first().toFile().toHash())
                    }
            }
        }
        .also {
            log.debug("Discovered the following tests: $it")
        }

    private fun TestConfig.getGeneralConfigOrNull() = pluginConfigs.filterIsInstance<GeneralConfig>().singleOrNull()

    companion object {
        private val log = LoggerFactory.getLogger(TestDiscoveringService::class.java)
    }
}
