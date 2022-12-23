package com.saveourtool.save.test.analysis.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/**
 * @see TestIdGenerator
 */
@TestMethodOrder(DisplayName::class)
class TestIdGeneratorTest {
    private lateinit var testIdGenerator: TestIdGenerator

    @BeforeEach
    fun beforeEach() {
        testIdGenerator = TestIdGenerator()
    }

    @Test
    fun `repeated invocations with the same arguments should result in the same TestId`() {
        val organizationName = "organizationName"
        val projectName = "projectName"
        val testSuiteSourceName = "testSuiteSourceName"
        val testSuiteVersion = "testSuiteVersion"
        val testSuiteName = "testSuiteName"
        val pluginName = "pluginName"
        val filePath = "filePath"

        val testId0 = TestId(
            organizationName = organizationName,
            projectName = projectName,
            testSuiteSourceName = testSuiteSourceName,
            testSuiteVersion = testSuiteVersion,
            testSuiteName = testSuiteName,
            pluginName = pluginName,
            filePath = filePath
        )

        val testId1 = TestId(
            organizationName = organizationName,
            projectName = projectName,
            testSuiteSourceName = testSuiteSourceName,
            testSuiteVersion = testSuiteVersion,
            testSuiteName = testSuiteName,
            pluginName = pluginName,
            filePath = filePath
        )

        assertThat(testId0)
            .isNotSameAs(testId1)
            .isEqualTo(testId1)
    }

    /**
     * Prevents from a naïve id generation algorithm, when all string arguments
     * might have been concatenated into a single string (or a byte buffer) and
     * hashed.
     */
    @Test
    fun `arguments should not be concatenated when hashing`() {
        val testId0 = TestId(
            projectName = "projectName",
            testSuiteSourceName = "",
        )

        val testId1 = TestId(
            projectName = "",
            testSuiteSourceName = "projectName",
        )

        val testId2 = TestId(
            projectName = "proje",
            testSuiteSourceName = "ctName",
        )

        assertThat(testId0).isNotEqualTo(testId1)
        assertThat(testId1).isNotEqualTo(testId2)
        assertThat(testId2).isNotEqualTo(testId0)
    }

    @Test
    fun `nulls should be different from an empty string when hashing`() {
        val organizationName = "organizationName"
        val projectName = "projectName"
        val testSuiteVersion = "testSuiteVersion"
        val testSuiteName = "testSuiteName"
        val pluginName = "pluginName"
        val filePath = "filePath"

        val testId0 = TestId(
            organizationName = organizationName,
            projectName = projectName,
            testSuiteSourceName = "",
            testSuiteVersion = testSuiteVersion,
            testSuiteName = testSuiteName,
            pluginName = pluginName,
            filePath = filePath
        )

        val testId1 = TestId(
            organizationName = organizationName,
            projectName = projectName,
            testSuiteSourceName = null,
            testSuiteVersion = testSuiteVersion,
            testSuiteName = testSuiteName,
            pluginName = pluginName,
            filePath = filePath
        )

        assertThat(testId0)
            .isNotEqualTo(testId1)
    }

    @Test
    fun `different organization names should result in different test ids`() {
        val testId0 = TestId(
            organizationName = "Organization A",
        )

        val testId1 = TestId(
            organizationName = "Organization B",
        )

        assertThat(testId0)
            .isNotEqualTo(testId1)
    }

    @Test
    fun `different project names should result in different test ids`() {
        val testId0 = TestId(
            projectName = "Project A",
        )

        val testId1 = TestId(
            projectName = "Project B",
        )

        assertThat(testId0)
            .isNotEqualTo(testId1)
    }

    @Test
    fun `different test suite source names should result in different test ids`() {
        val testId0 = TestId(
            testSuiteSourceName = "Test Suite Source A",
        )

        val testId1 = TestId(
            testSuiteSourceName = "Test Suite Source B",
        )

        assertThat(testId0)
            .isNotEqualTo(testId1)
    }

    @Test
    fun `different test suite versions should result in different test ids`() {
        val testId0 = TestId(
            testSuiteVersion = "v1",
        )

        val testId1 = TestId(
            testSuiteVersion = "v2",
        )

        assertThat(testId0)
            .isNotEqualTo(testId1)
    }

    @Test
    fun `different test suite names should result in different test ids`() {
        val testId0 = TestId(
            testSuiteName = "Test Suite Name A",
        )

        val testId1 = TestId(
            testSuiteName = "Test Suite Name B",
        )

        assertThat(testId0)
            .isNotEqualTo(testId1)
    }

    @Test
    fun `different plugin names should result in different test ids`() {
        val testId0 = TestId(
            pluginName = "Warn",
        )

        val testId1 = TestId(
            pluginName = "Fix",
        )

        assertThat(testId0)
            .isNotEqualTo(testId1)
    }

    @Test
    fun `different file paths should result in different test ids`() {
        val testId0 = TestId(
            filePath = "src/main/java",
        )

        val testId1 = TestId(
            filePath = "src/main/kotlin",
        )

        assertThat(testId0)
            .isNotEqualTo(testId1)
    }

    @Suppress(
        "TestFunctionName",
        "LongParameterList",
        "FUNCTION_NAME_INCORRECT_CASE",
        "TOO_MANY_PARAMETERS",
    )
    private fun TestId(
        organizationName: String = "",
        projectName: String = "",
        testSuiteSourceName: String? = "",
        testSuiteVersion: String? = "",
        testSuiteName: String? = "",
        pluginName: String = "",
        filePath: String = "",
    ): TestId =
            with(testIdGenerator) {
                testId(
                    organizationName = organizationName,
                    projectName = projectName,
                    testSuiteSourceName = testSuiteSourceName,
                    testSuiteVersion = testSuiteVersion,
                    testSuiteName = testSuiteName,
                    pluginName = pluginName,
                    filePath = filePath,
                )
            }
}
