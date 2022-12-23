package com.saveourtool.save.test.analysis.api

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
