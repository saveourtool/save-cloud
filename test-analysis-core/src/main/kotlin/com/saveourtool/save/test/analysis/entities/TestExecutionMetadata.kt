package com.saveourtool.save.test.analysis.entities

/**
 * Metadata of a test execution.
 *
 * @param organizationName the name of the organization.
 * @param projectName the name of the project.
 * @param testSuiteSourceName the name of the test suite source.
 * @param testSuiteVersion the version of the test suite.
 * @property testSuiteName the name of the test suite.
 * @property pluginName the name of the `save-cli` plug-in ("warn", "fix", etc.).
 * @property filePath the path to the file within the test suite.
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
)
class TestExecutionMetadata(
    organizationName: OrganizationName,
    projectName: ProjectName,
    testSuiteSourceName: TestSuiteSourceName?,
    testSuiteVersion: TestSuiteVersion?,
    val testSuiteName: TestSuiteName?,
    val pluginName: PluginName,
    val filePath: FilePath,
) : ExecutionMetadata(
    organizationName,
    projectName,
    testSuiteSourceName,
    testSuiteVersion,
)
