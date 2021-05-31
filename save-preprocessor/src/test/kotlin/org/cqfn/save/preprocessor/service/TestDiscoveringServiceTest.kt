package org.cqfn.save.preprocessor.service

import org.cqfn.save.entities.Project
import org.cqfn.save.preprocessor.config.ConfigProperties
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

    @BeforeAll
    fun setUp() {
        tmpDir = createTempDirectory(this::class.simpleName)
        Git.cloneRepository()
            .setURI("https://github.com/cqfn/save")
            .setDirectory(tmpDir.toFile())
            .call()
    }

    @AfterAll
    fun tearDown() {
        tmpDir.toFile().deleteRecursively()
    }

    @Test
    fun `should discover test suites`() {
        val testSuites = testDiscoveringService.getAllTestSuites(
            Project("stub", "stub", "stub", null),
            (tmpDir.resolve("examples")).toString()
        )

        Assertions.assertTrue(testSuites.isNotEmpty())  // fixme: check actual test suites when we properly use GeneralConfig in service
    }

    @Test
    fun `should throw exception with invalid path for test suites discovering`() {
        assertThrows<IllegalArgumentException> {
            testDiscoveringService.getAllTestSuites(
                Project("stub", "stub", "stub", null),
                (tmpDir.resolve("buildSrc")).toString()
            )
        }
    }
}
