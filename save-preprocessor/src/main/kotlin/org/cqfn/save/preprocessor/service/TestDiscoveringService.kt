package org.cqfn.save.preprocessor.service

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.core.utils.createPluginConfigListFromToml
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.plugin.warn.WarnPlugin
import org.cqfn.save.plugins.fix.FixPlugin
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.utils.toHash
import org.cqfn.save.test.TestDto
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType

import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath
import org.springframework.stereotype.Service

/**
 * A service that call SAVE core to discover test suites and tests
 */
@Service
class TestDiscoveringService(private val configProperties: ConfigProperties) {
    private val configDetector = ConfigDetector()

    /**
     * Returns a root config of hierarchy; this config will already have all descendants merged with their parents.
     *
     * @param testResourcesRootAbsolutePath path to directory with root config
     * @return a root [TestConfig]
     */
    @OptIn(ExperimentalFileSystem::class)
    fun getRootTestConfig(testResourcesRootAbsolutePath: String): TestConfig {
        val configDetector = ConfigDetector()
        val rootTestConfig = configDetector.configFromFile(testResourcesRootAbsolutePath.toPath())
        rootTestConfig.getAllTestConfigs().forEach { testConfig ->
            // discover plugins from the test configuration
            createPluginConfigListFromToml(testConfig.location).forEach {
                testConfig.pluginConfigs.add(it)
            }
            testConfig
                // merge configurations with parents (in place)
                .mergeConfigWithParents()
        }
        return rootTestConfig
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
    @OptIn(ExperimentalFileSystem::class)
    @Suppress("UnsafeCallOnNullableType")
    fun getAllTestSuites(project: Project, rootTestConfig: TestConfig, propertiesRelativePath: String) = rootTestConfig.mapDescendantsNotNull { testConfig ->
        testConfig.pluginConfigs
            .filterIsInstance<GeneralConfig>()
            .singleOrNull()
    }
        .map { generalConfig: GeneralConfig ->
            TestSuiteDto(TestSuiteType.PROJECT, generalConfig.suiteName!!, project, propertiesRelativePath)
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
    @OptIn(ExperimentalFileSystem::class)
    @Suppress("UnsafeCallOnNullableType")
    fun getAllTests(rootTestConfig: TestConfig, testSuites: List<TestSuite>) = rootTestConfig.flatMapDescendants { testConfig ->
        val plugins: List<Plugin> = testConfig.pluginConfigs
            .filterNot { it is GeneralConfig }
            .map { pluginConfig ->
                when (pluginConfig.type) {
                    TestConfigSections.FIX -> FixPlugin(testConfig)
                    TestConfigSections.WARN -> WarnPlugin(testConfig)
                    else -> throw PluginException("Unknown type <${pluginConfig::class}> of plugin config was provided")
                }
            }
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()
        val testSuite = testSuites.first { it.name == generalConfig.suiteName }
        plugins.flatMap { plugin ->
            plugin.discoverTestFiles(testConfig.directory).map {
                TestDto(it.first().toString(), testSuite.id!!, it.first().toFile().toHash())
            }
        }
    }
        .toList()
}

/**
 * Applies the transformation to all descendant [TestConfig]s.
 * FixMe: Use configs traversal after it's implemented is SAVE.
 *
 * @param transform a function to invoke on all configs
 * @return a sequence of transformed configs
 */
fun <T> TestConfig.mapDescendantsNotNull(transform: (TestConfig) -> T?): Sequence<T> = sequence {
    transform(this@mapDescendantsNotNull)?.let { yield(it) }
    childConfigs.map {
        yieldAll(it.mapDescendantsNotNull(transform))
    }
}

/**
 * Applies the transformation to all descendant [TestConfig]s and returns a flattened sequence.
 * FixMe: Use configs traversal after it's implemented is SAVE.
 *
 * @param transform a function to invoke on all configs
 * @return a sequence of transformed configs
 */
fun <T> TestConfig.flatMapDescendants(transform: (TestConfig) -> Iterable<T>): Sequence<T> = sequence {
    yieldAll(transform(this@flatMapDescendants))
    childConfigs.map {
        yieldAll(it.flatMapDescendants(transform))
    }
}
