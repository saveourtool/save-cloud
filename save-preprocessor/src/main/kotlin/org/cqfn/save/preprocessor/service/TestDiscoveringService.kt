package org.cqfn.save.preprocessor.service

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.utils.toHash
import org.cqfn.save.test.TestDto
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType

import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath
import org.springframework.stereotype.Service

import java.io.File

/**
 * A service that call SAVE core to discover test suites and tests
 */
@Service
class TestDiscoveringService(private val configProperties: ConfigProperties) {
    private val configDetector = ConfigDetector()

    /**
     * Discover all test suites in the project
     *
     * @param project a [Project] corresponding to analyzed data
     * @param path path to the root of test resources
     * @return a list of [TestSuiteDto]s
     */
    @OptIn(ExperimentalFileSystem::class)
    fun getAllTestSuites(project: Project, path: String): List<TestSuiteDto> {
        val absolutePath = File(configProperties.repository, path).absolutePath
        val rootTestConfig = configDetector.configFromFile(absolutePath.toPath()) ?: error("SAVE config not found in $absolutePath")
        return rootTestConfig.mapDescendants {
            val generalConfig = GeneralConfig("stub")  // todo: discover general config
            TestSuiteDto(TestSuiteType.PROJECT, generalConfig.suiteName, project)
        }
            .toList()
    }

    /**
     * Discover all tests in the project
     *
     * @param path path to the root of test resources
     * @param testSuites testSuites in this project
     * @return a list of [TestDto]s
     */
    @OptIn(ExperimentalFileSystem::class)
    fun getAllTests(path: String, testSuites: List<TestSuite>): List<TestDto> {
        val absolutePath = File(configProperties.repository, path).absolutePath
        val rootTestConfig = configDetector.configFromFile(absolutePath.toPath()) ?: error("SAVE config not found in $absolutePath")
        return rootTestConfig.flatMapDescendants { config ->
            // todo: should get a list of plugins from config
            val plugins: List<Plugin> = emptyList()
            val generalConfig = GeneralConfig("stub")  // todo: discover general config
            val testSuite = testSuites.first { it.name == generalConfig.suiteName }
            plugins.flatMap {
                it.discoverTestFiles(absolutePath.toPath()).map {
                    TestDto(it.first().name, testSuite.id!!, it.first().toFile().toHash())
                }
            }
        }.toList()
    }

    /**
     * Applies the transformation to all descendant [TestConfig]s
     *
     * @param transform a function to invoke on all configs
     * @return a sequence of transformed configs
     */
    // todo: implement in SAVE?
    fun <T> TestConfig.mapDescendants(transform: (TestConfig) -> T): Sequence<T> = sequence {
        yield(transform(this@mapDescendants))
        childConfigs.map {
            yieldAll(it.mapDescendants(transform))
        }
    }

    /**
     * Applies the transformation to all descendant [TestConfig]s and returns a flattened sequence
     *
     * @param transform a function to invoke on all configs
     * @return a sequence of transformed configs
     */
    // todo: implement in SAVE?
    fun <T> TestConfig.flatMapDescendants(transform: (TestConfig) -> Iterable<T>): Sequence<T> = sequence {
        yieldAll(transform(this@flatMapDescendants))
        childConfigs.map {
            yieldAll(it.flatMapDescendants(transform))
        }
    }
}
