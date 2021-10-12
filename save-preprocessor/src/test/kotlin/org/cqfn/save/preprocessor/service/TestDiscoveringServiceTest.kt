package org.cqfn.save.preprocessor.service

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.testsuite.TestSuiteType
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.File
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path

@ExtendWith(SpringExtension::class)
@EnableConfigurationProperties(ConfigProperties::class)
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestDiscoveringService::class)
class TestDiscoveringServiceTest {
    private val propertiesRelativePath = "examples/kotlin-diktat/save.properties"
    @Autowired private lateinit var testDiscoveringService: TestDiscoveringService
    private lateinit var tmpDir: Path
    private lateinit var rootTestConfig: TestConfig

    @BeforeAll
    fun setUp() {
        tmpDir = createTempDirectory(this::class.simpleName)
        Git.cloneRepository()
            .setURI("https://github.com/cqfn/save")
            .setDirectory(tmpDir.toFile())
            .call()
        rootTestConfig = testDiscoveringService.getRootTestConfig(tmpDir.resolve("examples/kotlin-diktat").toString())
    }

    @AfterAll
    fun tearDown() {
        tmpDir.toFile().deleteRecursively()
    }

    @Test
    fun `should discover test suites`() {
        val testSuites = testDiscoveringService.getAllTestSuites(
            Project("stub", "stub", "stub", null),
            rootTestConfig,
            propertiesRelativePath,
            "not-provided"
        )

        println("Discovered test suites: $testSuites")
        Assertions.assertTrue(testSuites.isNotEmpty())
        Assertions.assertEquals("Autofix: Smoke Tests", testSuites.first().name)
    }

    @Test
    fun `should throw exception with invalid path for test suites discovering`() {
        assertThrows<IllegalArgumentException> {
            testDiscoveringService.getAllTestSuites(
                Project("stub", "stub", "stub", null),
                testDiscoveringService.getRootTestConfig(tmpDir.resolve("buildSrc").toString()),
                propertiesRelativePath,
                "not-provided"
            )
        }
    }

    @Test
    fun `should discover tests`() {
        val testDtos = testDiscoveringService.getAllTests(
            rootTestConfig,
            listOf(
                createTestSuiteStub("Autofix: Smoke Tests", 1),
                createTestSuiteStub("DocsCheck", 2),
                createTestSuiteStub("Only Warnings: General", 3),
                createTestSuiteStub("Autofix and Warn", 4),
                createTestSuiteStub("Directory: Chapter 1", 5),
                createTestSuiteStub("Directory: Chapter2", 6),
                createTestSuiteStub("Directory: Chapter3", 7),
            )
        )

        println("Discovered the following tests: $testDtos")
        Assertions.assertEquals(13, testDtos.size)
        Assertions.assertEquals(testDtos.size, testDtos.map { it.hash + it.filePath + it.testSuiteId }.distinct().size) {
            "Some tests have the same hash/filePath/testSuiteId combination in $testDtos"
        }
        Assertions.assertTrue(testDtos.none { File(it.filePath).isAbsolute }) {
            "Test should be stored with paths relative to their root config, but some are stored with absolute paths: $testDtos"
        }
    }

    private fun createTestSuiteStub(name: String, id: Long) = TestSuite(TestSuiteType.PROJECT, name, null, null, null, propertiesRelativePath).apply {
        this.id = id
    }
}
