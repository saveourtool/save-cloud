package com.saveourtool.save.preprocessor.test.suite

import com.saveourtool.save.test.TestSuiteValidationProgress
import com.saveourtool.save.test.TestSuiteValidationResult
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.getLogger

/**
 * Plug-ins without tests.
 */
@TestSuiteValidatorComponent
class PluginsWithoutTests : AbstractTestSuiteValidator() {
    override fun validate(
        testSuites: List<TestSuiteDto>,
        onStatusUpdate: (status: TestSuiteValidationResult) -> Unit,
    ) {
        require(testSuites.isNotEmpty())

        @Suppress("MAGIC_NUMBER")
        for (i in 0..10) {
            val status = TestSuiteValidationProgress(javaClass.name, CHECK_NAME, i * 10)
            logger.info("Emitting \"$status\"...")
            onStatusUpdate(status)
            Thread.sleep(500L)
        }
    }

    private companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<PluginsWithoutTests>()
        private const val CHECK_NAME = "Searching for plug-ins with zero tests"
    }
}
