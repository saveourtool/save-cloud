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
            propertiesRelativePath
        )

        println("Discovered test suites: $testSuites")
        Assertions.assertTrue(testSuites.isNotEmpty())
        Assertions.assertEquals("autofix", testSuites.first().name)
    }

    @Test
    fun `should throw exception with invalid path for test suites discovering`() {
        assertThrows<IllegalArgumentException> {
            testDiscoveringService.getAllTestSuites(
                Project("stub", "stub", "stub", null),
                testDiscoveringService.getRootTestConfig(tmpDir.resolve("buildSrc").toString()),
                propertiesRelativePath
            )
        }
    }

    @Test
    fun `should discover tests`() {
        val testDtos = testDiscoveringService.getAllTests(
            rootTestConfig,
            listOf(
                createTestSuiteStub("smoke tests", 1),
                createTestSuiteStub("autofix", 2),
                createTestSuiteStub("DocsCheck", 3),
            )
        )

        println("Discovered the following tests: $testDtos")
        Assertions.assertEquals(2, testDtos.size)
        Assertions.assertEquals(testDtos.size, testDtos.map { it.hash }.distinct().size) {
            "Some tests have the same hash in $testDtos"
        }
        Assertions.assertEquals("Example1Expected.kt", File(testDtos.first().filePath).name)
    }

    private fun createTestSuiteStub(name: String, id: Long) = TestSuite(TestSuiteType.PROJECT, name, null, null, propertiesRelativePath).apply {
        this.id = id
    }
}
