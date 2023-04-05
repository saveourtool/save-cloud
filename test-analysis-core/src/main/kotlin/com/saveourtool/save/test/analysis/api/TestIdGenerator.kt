@file:Suppress("EXTENSION_FUNCTION_WITH_CLASS")

package com.saveourtool.save.test.analysis.api

import com.saveourtool.save.test.analysis.entities.FilePath
import com.saveourtool.save.test.analysis.entities.OrganizationName
import com.saveourtool.save.test.analysis.entities.PluginName
import com.saveourtool.save.test.analysis.entities.ProjectName
import com.saveourtool.save.test.analysis.entities.TestExecutionMetadata
import com.saveourtool.save.test.analysis.entities.TestSuiteName
import com.saveourtool.save.test.analysis.entities.TestSuiteSourceName
import com.saveourtool.save.test.analysis.entities.TestSuiteVersion
import com.saveourtool.save.test.analysis.internal.DefaultTestIdGenerator

/**
 * Generates unique test ids.
 */
fun interface TestIdGenerator {
    /**
     * Generates a unique test id.
     *
     * @param organizationName the name of the organization.
     * @param projectName the name of the project.
     * @param testSuiteSourceName the name of the test suite source.
     * @param testSuiteVersion the version of the test suite.
     * @param testSuiteName the name of the test suite.
     * @param pluginName the name of the `save-cli` plug-in ("warn", "fix", etc.).
     * @param filePath the path to the file within the test suite.
     * @return the generated unique test id.
     */
    @Suppress(
        "LongParameterList",
        "TOO_MANY_PARAMETERS",
    )
    fun testId(
        organizationName: String,
        projectName: String,
        testSuiteSourceName: String?,
        testSuiteVersion: String?,
        testSuiteName: String?,
        pluginName: String,
        filePath: String,
    ): TestId

    companion object {
        /**
         * Creates a new service instance.
         *
         * @return a new instance of the default implementation.
         */
        operator fun invoke(): TestIdGenerator =
                DefaultTestIdGenerator()
    }
}

/**
 * Generates a unique test id.
 *
 * @param organizationName the name of the organization.
 * @param projectName the name of the project.
 * @param testSuiteSourceName the name of the test suite source.
 * @param testSuiteVersion the version of the test suite.
 * @param testSuiteName the name of the test suite.
 * @param pluginName the name of the `save-cli` plug-in ("warn", "fix", etc.).
 * @param filePath the path to the file within the test suite.
 * @return the generated unique test id.
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
)
fun TestIdGenerator.testId(
    organizationName: OrganizationName,
    projectName: ProjectName,
    testSuiteSourceName: TestSuiteSourceName?,
    testSuiteVersion: TestSuiteVersion?,
    testSuiteName: TestSuiteName?,
    pluginName: PluginName,
    filePath: FilePath,
): TestId =
        testId(
            organizationName.value,
            projectName.value,
            testSuiteSourceName?.value,
            testSuiteVersion?.value,
            testSuiteName?.value,
            pluginName.value,
            filePath.value,
        )

/**
 * Generates a unique test id.
 *
 * @param metadata test execution metadata.
 * @return the generated unique test id.
 */
fun TestIdGenerator.testId(metadata: TestExecutionMetadata): TestId =
        testId(
            metadata.organizationName,
            metadata.projectName,
            metadata.testSuiteSourceName,
            metadata.testSuiteVersion,
            metadata.testSuiteName,
            metadata.pluginName,
            metadata.filePath,
        )
