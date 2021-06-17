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
        rootTestConfig = testDiscoveringService.getRootTestConfig(tmpDir.resolve("examples").toString())
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
            "examples/save.properties"
        )

        println("Discovered test suites: $testSuites")
        Assertions.assertTrue(testSuites.isNotEmpty())
        Assertions.assertEquals("\"DocsCheck\"", testSuites.first().name)
    }

    @Test
    fun `should throw exception with invalid path for test suites discovering`() {
        assertThrows<IllegalArgumentException> {
            testDiscoveringService.getAllTestSuites(
                Project("stub", "stub", "stub", null),
                testDiscoveringService.getRootTestConfig(tmpDir.resolve("buildSrc").toString()),
                "examples/save.properties"
            )
        }
    }

    @Test
    fun `should discover tests`() {
        val testDtos = testDiscoveringService.getAllTests(
            rootTestConfig,
            listOf(
                TestSuite(TestSuiteType.PROJECT, "\"DocsCheck\"", null, null, "examples/save.properties").apply {
                    id = 1
                }
            )
        )

        println(testDtos)
        Assertions.assertEquals(2, testDtos.size)
        Assertions.assertEquals("MyTest.java", File(testDtos.first().filePath).name)
    }
}
