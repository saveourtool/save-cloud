/**
 * Mapping entities from SAVE-core to their equivalents from SAVE-cloud
 */

package org.cqfn.save.utils

import org.cqfn.save.core.logging.logTrace
import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.domain.TestResultStatus

import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath

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
 * Trims leading [PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE] from paths to resources of current [TestResult]
 *
 * @return a copy of [TestResult] with adjusted resource paths
 */
@OptIn(ExperimentalFileSystem::class)
fun TestResult.withAdjustedLocation(): TestResult {
    // In standard mode we have extra paths in json reporter, since we created extra directories,
    // and this information won't be matched with data from DB without such removal
    val location = resources.test.parent!!.toString()
    return if (location.startsWith(PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE)) {
        logTrace("Adjusting path to test file [$location/${resources.test.name}]: trimming $PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE")
        val actualTestRoot = location.split('/').first().toPath()
        this.copy(resources = resources.withRelativePaths(actualTestRoot))
    } else {
        // Use filePath as is for Git mode
        this
    }
}

/**
 * Maps `TestResult` to `TestResultDebugInfo`
 *
 * @param testSuiteName name of the test suite
 * @param pluginName name of the plugin that has been executed
 * @return an instance of [TestResultDebugInfo] representing execution info
 */
@OptIn(ExperimentalFileSystem::class)
fun TestResult.toTestResultDebugInfo(testSuiteName: String, pluginName: String): TestResultDebugInfo = TestResultDebugInfo(
    TestResultLocation(
        testSuiteName,
        pluginName,
        resources.test.parent!!.toString(),
        resources.test.name
    ),
    debugInfo,
    status,
)
