/**
 * Mapping entities from SAVE-cli to their equivalents from SAVE-cloud
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.core.result.*
import com.saveourtool.save.domain.TestResultStatus

/**
 * Maps `TestStatus` to `TestResultStatus`
 */
fun TestStatus.toTestResultStatus() = when (this) {
    is Pass -> TestResultStatus.PASSED
    is Fail -> TestResultStatus.FAILED
    is Ignored -> TestResultStatus.IGNORED
    is Crash -> TestResultStatus.TEST_ERROR
    else -> error("Unknown test status $this")
}

/**
 * Maps `TestResult` to `TestResultDebugInfo`
 *
 * @param testSuiteName name of the test suite
 * @param pluginName name of the plugin that has been executed
 * @return an instance of [TestResultDebugInfo] representing execution info
 */
fun TestResult.toTestResultDebugInfo(testSuiteName: String, pluginName: String): TestResultDebugInfo = TestResultDebugInfo(
    TestResultLocation(
        testSuiteName = testSuiteName,
        pluginName = pluginName,
        testPath = resources.test.toString(),
    ),
    debugInfo,
    status,
)
