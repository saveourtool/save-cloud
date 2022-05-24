/**
 * Mapping entities from SAVE-core to their equivalents from SAVE-cloud
 */

package com.saveourtool.save.utils

import com.saveourtool.save.core.logging.logTrace
import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestResult
import com.saveourtool.save.core.result.TestStatus
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
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
fun TestResult.toTestResultDebugInfo(testSuiteName: String, pluginName: String): TestResultDebugInfo {
    // In standard mode we have extra paths in json reporter, since we created extra directories,
    // and this information won't be matched with data from DB without such removal
    val location = resources.test.parent!!.toString()
    val adjustedLocation = adjustLocation(location)
    return TestResultDebugInfo(
        TestResultLocation(
            testSuiteName,
            pluginName,
            adjustedLocation,
            resources.test.name
        ),
        debugInfo,
        status,
    )
}

/**
 * @param location location to be processed
 */
fun adjustLocation(location: String) = if (location.startsWith(PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE)) {
    logTrace("Adjusting path to [$location]: trimming $PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE")
    location.dropWhile { it != '/' }.drop(1)
} else {
    // Use filePath as is for Git mode
    location
}
